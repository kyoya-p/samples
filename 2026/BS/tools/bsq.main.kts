#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-io-core:0.8.2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
@file:DependsOn("io.ktor:ktor-client-core-jvm:3.0.3")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.0.3")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:3.0.3")
@file:DependsOn("io.ktor:ktor-serialization-kotlinx-json-jvm:3.0.3")
@file:DependsOn("com.fleeksoft.ksoup:ksoup-jvm:0.1.2")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.16")

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.TextNode
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.client.plugins.contentnegotiation.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.buffered
import kotlinx.io.writeString

// --- Configuration & Global Client ---

val client = HttpClient(CIO) {
    followRedirects = true
    install(ContentNegotiation) {
        json(Json { prettyPrint = true; ignoreUnknownKeys = true })
    }
}

// --- Data Structures (Per Readme) ---

typealias CardId = String

@Serializable
data class Card(
    val id: String, 
    val name: String,
    // Store internal state for full caching
    val category: String = "S",
    val attr: String = "",
    val cost: Int = 0,
    val symbol: String = "",
    val reduction: String = "",
    val family: List<String> = emptyList(),
    val lvCost: List<String> = emptyList(),
    val effect: String = ""
)

// --- Helper for File Operations (kotlinx-io) ---

fun saveJson(filePath: String, jsonObject: JsonObject) {
    val path = Path(filePath)
    // Create parent directory if needed
    val parent = Path("cards")
    if (!SystemFileSystem.exists(parent)) {
        SystemFileSystem.createDirectories(parent)
    }
    
    val json = Json { prettyPrint = true }
    val content = json.encodeToString(JsonObject.serializer(), jsonObject)
    
    SystemFileSystem.sink(path).buffered().use { sink ->
        sink.writeString(content)
    }
}

// --- Logic Functions (Per Readme I/F) ---

suspend fun listCards(kfreeKyword: String): List<CardId> {
    val response: HttpResponse = client.post("https://www.battlespirits.com/cardlist/index.php?search=true") {
        setBody(FormDataContent(Parameters.build {
            append("freewords", kfreeKyword)
            append("view_switch", "on") 
        }))
    }

    val listHtml = response.bodyAsText()
    val doc = Ksoup.parse(listHtml)
    
    val ids = mutableListOf<CardId>()
    doc.select("span.num").forEach { 
        val id = it.text().trim()
        if (id.isNotEmpty()) ids.add(id)
    }
    
    return ids
}

suspend fun Card.updateCache(cardId: CardId): Card {
    val detailUrl = "https://www.battlespirits.com/cardlist/detail_iframe.php?card_no=$cardId&card_no2=$cardId"
    
    val response = client.get(detailUrl)
    val html = response.bodyAsText()
    val doc = Ksoup.parse(html)
    
    // Selectors adjusted based on HAR:
    // Name: h2.cardName
    // Cost: dd.costVal
    // Attr: span.attribute
    // System: span.system
    // Reduction: dd.alleviationVal
    
    val fetchedName = doc.select("h2.cardName").text().trim()
    val finalName = if (fetchedName.isNotEmpty()) fetchedName else this.name
    
    val cost = doc.select("dd.costVal").text().toIntOrNull() ?: 0
    val attr = doc.select("span.attribute").text().trim()
    val system = doc.select("span.system").text().trim()
    
    val lvCostList = mutableListOf<String>()
    doc.select("div.bpCoreItem").forEach { item ->
        val lv = item.select("img[alt^=LV]").attr("alt").replace("LV", "")
        val bp = item.select("span.bpCoreVal").text()
        val core = item.select("span.bpCoreLevel").text()
        if (lv.isNotEmpty()) lvCostList.add("$lv,$core,$bp")
    }
    
    var effectText = "取得失敗"
    val effectDd = doc.select("dt.detailColListTerm:contains(能力・効果) + dd.detailColListDescription").first()
    if (effectDd != null) {
        effectDd.select("img").forEach { img ->
            val alt = img.attr("alt")
            img.replaceWith(TextNode(if (alt.isNotEmpty()) "[$alt]" else "[アイコン]"))
        }
        effectDd.select("br").forEach { br -> br.replaceWith(TextNode("\n")) }
        effectText = effectDd.text().trim()
    }
    
    val reductionMap = mutableMapOf<String, Int>()
    doc.select("dd.alleviationVal img").forEach { img ->
        val key = if (img.attr("alt") == "◇") "全" else img.attr("alt")
        if (key.isNotEmpty()) reductionMap[key] = reductionMap.getOrDefault(key, 0) + 1
    }
    val reductionStr = reductionMap.entries.joinToString("") { "${it.key}${it.value}" }
    
    val familyList = system.split("・").filter { it.isNotBlank() }
    
    val fullCard = Card(
        id = cardId,
        name = finalName,
        attr = attr,
        cost = cost,
        symbol = if (attr.isNotEmpty()) "${attr.take(1)}1" else "",
        reduction = reductionStr,
        family = familyList,
        lvCost = lvCostList,
        effect = effectText
    )
    
    // Construct JSON for database (GEMINI.md compliant)
    val jsonObject = buildJsonObject {
        put("id", fullCard.id)
        put("cardName", fullCard.name)
        put("category", fullCard.category)
        put("attr", fullCard.attr)
        put("cost", fullCard.cost)
        put("symbol", fullCard.symbol)
        put("reduction", fullCard.reduction)
        putJsonArray("family") { fullCard.family.forEach { add(it) } }
        putJsonArray("lvCost") { fullCard.lvCost.forEach { add(it) } }
        put("effect", fullCard.effect)
        put("note", "")
    }
    
    val safeName = finalName.replace("/", "_")
    val familyStr = familyList.joinToString(",")
    val fileName = "cards/$cardId.$safeName.${fullCard.category}.$attr.$familyStr.json"
    
    saveJson(fileName, jsonObject)
    
    return fullCard
}

// --- Main CLI Entry ---

val argsList = args.toList()
val showCount = argsList.contains("-n")
val listOnly = argsList.contains("-l")
val help = argsList.contains("-h") || argsList.isEmpty()

val keywords = argsList.filter { !it.startsWith("-") }

if (help) {
    println("Usage: kotlin bsq.main.kts [-h] [-n] [-l] [Keyword ...]")
    println("  -h: Show help")
    println("  -n: Show count of results")
    println("  -l: List basic info (ID, Name) only")
    System.exit(0)
}

runBlocking {
    try {
        val searchWords = keywords.joinToString(" ")
        val ids = listCards(searchWords)
        
        if (showCount) {
            println("Found ${ids.size} cards.")
        }
        
        for (id in ids) {
            val baseCard = Card(id, "Loading...")
            val fullCard = baseCard.updateCache(id)
            
            if (listOnly || !showCount) {
                println("${fullCard.id} ${fullCard.name}")
            }
            
            delay(200) 
        }
        
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        client.close()
    }
}

#!/usr/bin/env kotlin

@file:Repository("https://repo.maven.apache.org/maven2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-io-core:0.8.1")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
@file:DependsOn("io.ktor:ktor-client-core-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-client-content-negotiation-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-serialization-kotlinx-json-jvm:3.3.3")
@file:DependsOn("com.fleeksoft.ksoup:ksoup-jvm:0.2.5")
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

// --- Global Client ---
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
    val category: String ,
    val attr: String ,
    val cost: Int ,
    val symbol: String ,
    val reduction: String ,
    val family: List<String> ,
    val lvCost: List<String> ,
    val effect: String ,
)

// --- Argument Parsing ---

val argsList = args.toList()
val help = argsList.contains("-h") || argsList.isEmpty()
val showCount = argsList.contains("-n")
val listOnly = argsList.contains("-l")

var cacheDirStr: String = ""
val cIndex = argsList.indexOf("-c")
if (cIndex != -1 && cIndex + 1 < argsList.size) {
    cacheDirStr = argsList[cIndex + 1]
} else {
    // Default to ~/.bscards
    // Note: Use System.getProperty for home dir, avoiding java.io.File
    val home = System.getProperty("user.home")
    cacheDirStr = "$home/.bscards"
}

val keywords = argsList.filterIndexed { index, s -> 
    !s.startsWith("-") && (index == 0 || argsList[index-1] != "-c") 
}

if (help) {
    println("Usage: kotlin bsq.main.kts [-h] [-n] [-l] [-c キャッシュフォルダ] [Keyword ...]")
    println("  -h: Show help")
    println("  -n: Show count of results")
    println("  -l: List basic info (ID, Name) only")
    println("  -c: Specify cache directory (Default: ~/.bscards)")
    System.exit(0)
}

// --- Functions (Per Readme I/F) ---

suspend fun listCards(keywords: List<String>, httpClient: HttpClient): List<Card> {
    val kfreeKyword = keywords.joinToString(" ")
    val response: HttpResponse = httpClient.post("https://www.battlespirits.com/cardlist/index.php?search=true") {
        setBody(FormDataContent(Parameters.build {
            append("freewords", kfreeKyword)
            append("view_switch", "on") 
        }))
    }

    val listHtml = response.bodyAsText()
    val doc = Ksoup.parse(listHtml)
    
    val foundCards = mutableListOf<Card>()
    val items = doc.select("li.cardCol.js-detail")
    
    for (item in items) {
        val id = item.select("span.num").text().trim()
        if (id.isEmpty()) continue
        
        val name = item.select("h3.name").text().trim()
        foundCards.add(Card(id, name))
    }
    
    return foundCards
}

suspend fun Card.updateCache(httpClient: HttpClient) {
    val detailUrl = "https://www.battlespirits.com/cardlist/detail_iframe.php?card_no=${this.id}&card_no2=${this.id}"
    
    val response = httpClient.get(detailUrl)
    val html = response.bodyAsText()
    val doc = Ksoup.parse(html)
    
    // Selectors from HAR analysis
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
    
    // Final Data for JSON
    val category = "S" // Default
    val symbol = if (attr.isNotEmpty()) "${attr.take(1)}1" else ""
    
    val jsonObject = buildJsonObject {
        put("id", id)
        put("cardName", finalName)
        put("category", category)
        put("attr", attr)
        put("cost", cost)
        put("symbol", symbol)
        put("reduction", reductionStr)
        putJsonArray("family") { familyList.forEach { add(it) } }
        putJsonArray("lvCost") { lvCostList.forEach { add(it) } }
        put("effect", effectText)
        put("note", "")
    }
    
    // Save using kotlinx-io
    val baseDir = Path(cacheDirStr)
    if (!SystemFileSystem.exists(baseDir)) {
        SystemFileSystem.createDirectories(baseDir)
    }
    
    val safeName = finalName.replace("/", "_")
    val familyStr = familyList.joinToString(",")
    val fileName = "$id.$safeName.$category.$attr.$familyStr.json"
    val filePath = Path(baseDir, fileName)
    
    val json = Json { prettyPrint = true }
    val content = json.encodeToString(JsonObject.serializer(), jsonObject)
    
    SystemFileSystem.sink(filePath).buffered().use { sink ->
        sink.writeString(content)
    }
}

// --- Main Execution ---

runBlocking {
    try {
        val cards = listCards(keywords, client)
        
        if (showCount) {
            println("Found ${cards.size} cards.")
        }
        
        for (card in cards) {
            // Update cache (fetches details and saves file)
            card.updateCache(client)
            
            if (listOnly || !showCount) {
                println("${card.id} ${card.name}")
            }
            
            delay(200)
        }
        
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        client.close()
    }
}
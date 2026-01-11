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
    val category: String,
    val attr: String,
    val cost: Int,
    val symbol: String,
    val reduction: String,
    val family: List<String>,
    val lvCost: List<String>,
    val effect: String
)

// --- Argument Parsing ---

val argsList = args.toList()
val help = argsList.contains("-h") || argsList.isEmpty()
val showCount = argsList.contains("-n")
val listOnly = argsList.contains("-l")

val cIndex = argsList.indexOf("-c")
val cacheDirStr: String = if (cIndex != -1 && cIndex + 1 < argsList.size) {
    argsList[cIndex + 1]
} else {
    val home = System.getProperty("user.home")
    "$home/.bscards"
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
    
    // Construct Card with explicit empty/zero values since defaults are removed
    return doc.select("li.cardCol.js-detail").mapNotNull {
        val id = it.select("span.num").text().trim()
        if (id.isEmpty()) {
            null
        } else {
            val name = it.select("h3.name").text().trim()
            Card(
                id = id,
                name = name,
                category = "S", // Defaulting here as placeholder
                attr = "",
                cost = 0,
                symbol = "",
                reduction = "",
                family = emptyList(),
                lvCost = emptyList(),
                effect = ""
            )
        }
    }
}

suspend fun Card.updateCache(httpClient: HttpClient) {
    val detailUrl = "https://www.battlespirits.com/cardlist/detail_iframe.php?card_no=${this.id}&card_no2=${this.id}"
    
    val response = httpClient.get(detailUrl)
    val html = response.bodyAsText()
    val doc = Ksoup.parse(html)
    
    val fetchedName = doc.select(".cardName").first()?.text()?.trim() ?: ""
    val finalName = if (fetchedName.isNotEmpty()) fetchedName else this.name
    
    // Selectors based on standard Desktop layout (via UA)
    val cost = doc.select("dt:contains(コスト) + dd").first()?.text()?.trim()?.toIntOrNull() ?: 0
    val attr = doc.select(".attribute").first()?.text()?.trim() ?: ""
    val system = doc.select("dt:contains(系統) + dd").first()?.text()?.trim() ?: ""
    
    val lvCostList = doc.select(".bpCoreItem").mapNotNull { item ->
        val lv = item.select("img[alt^=LV]").attr("alt").replace("LV", "")
        val bp = item.select(".bpCoreVal").text()
        val core = item.select(".bpCoreLevel").text()
        if (lv.isNotEmpty()) "$lv,$core,$bp" else null
    }
    
    val effectText = doc.select(".detailColListTerm:contains(能力・効果) + .detailColListDescription").first()?.let { effectDd ->
        effectDd.select("img").forEach { img ->
            val alt = img.attr("alt")
            img.replaceWith(TextNode(if (alt.isNotEmpty()) "[$alt]" else "[アイコン]"))
        }
        effectDd.select("br").forEach { br -> br.replaceWith(TextNode("\n")) }
        effectDd.text().trim()
    } ?: "取得失敗"
    
    // Reduction: dt contains '軽減コスト' -> next dd -> images
    val reductionStr = doc.select("dt:contains(軽減コスト) + dd img")
        .map { img -> if (img.attr("alt") == "◇") "全" else img.attr("alt") }
        .filter { it.isNotEmpty() }
        .groupingBy { it }
        .eachCount()
        .entries.joinToString("") { "${it.key}${it.value}" }
    
    val familyList = system.split("・").filter { it.isNotBlank() }
    
    val categoryText = doc.select("dt:contains(カテゴリー) + dd").text().trim()
    val category = when {
        categoryText.contains("スピリット") -> "S"
        categoryText.contains("アルティメット") -> "U"
        categoryText.contains("ブレイヴ") -> "B"
        categoryText.contains("ネクサス") -> "N"
        categoryText.contains("マジック") -> "M"
        else -> "S"
    }
    val symbol = if (attr.isNotEmpty()) "${attr.take(1)}1" else ""
    
    // Create a full Card instance for serialization
    val fullCard = Card(
        id = id,
        name = finalName,
        category = category,
        attr = attr,
        cost = cost,
        symbol = symbol,
        reduction = reductionStr,
        family = familyList,
        lvCost = lvCostList,
        effect = effectText
    )
    
    // We add "note" field manually via JsonObjectBuilder if needed, or if Card matches JSON exactly.
    // Readme/GEMINI.md requirements for JSON format include "note".
    // Since Card class doesn't have "note", we'll build JsonObject.
    
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
    
    val baseDir = Path(cacheDirStr)
    if (!SystemFileSystem.exists(baseDir)) {
        SystemFileSystem.createDirectories(baseDir)
    }
    
    // Sanitize filename: remove / \ : * ? " < > | and limit length
    val sanitizedName = finalName.replace(Regex("[\\\\/:*?\"<>|\\s]"), "_").take(50)
    val familyStr = familyList.joinToString(",").replace(Regex("[\\\\/:*?\"<>|]"), "_").take(50)
    val fileName = "$id.$sanitizedName.$category.$attr.$familyStr.json"
    val filePath = Path(baseDir, fileName)
    
    val json = Json { prettyPrint = true }
    val content = json.encodeToString(JsonObject.serializer(), jsonObject)
    
    SystemFileSystem.sink(filePath).buffered().use {
        it.writeString(content)
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

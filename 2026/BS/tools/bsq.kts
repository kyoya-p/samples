/*
  指示(AI編集禁止)

  BSサイトに問い合わせる
  dependOn:
  - kotlinx.io
  - org.jetbrains.kotlinx:kotlinx-io-core:0.8.2
  - org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0
  - io.ktor:ktor-client:3.3.3
  - io.ktor:ktor-http:3.3.3
  - ksoap:

# usage
```
kotiln BSQ_official.kts [-h] [Keyword,Keyword,...]
```
-h またはパラメなし: ヘルプ表示

基本I/F:
val searchWords = args.map{it.toString}
typealias CardId = String
@Serializable data class Card(val id: String, val name: String)
suspend fun listCards(kfreeKyword: String): List<CardId> {}
suspend fun Card.updateCache(cardId: CardId) :Card {}

指示ここまで */

@file:Repository("https://repo.maven.apache.org/maven2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-io-core-jvm:0.6.0")
@file:DependsOn("io.ktor:ktor-client-core-jvm:3.0.3")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.0.3")
@file:DependsOn("io.ktor:ktor-http-jvm:3.0.3")
@file:DependsOn("org.slf4j:slf4j-simple:2.0.16")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.3")

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.charset.StandardCharsets

// Debug log
File("bsq_debug.log").appendText("Script loaded\n")
println("DEBUG: Script Start")

typealias CardId = String

@Serializable
data class Card(
    val id: String,
    val name: String,
    val category: String? = null,
    val cost: String? = null,
    val attr: String? = null,
    val family: String? = null,
    val effect: String? = null,
    val lvCost: List<String> = emptyList()
)

val client = HttpClient(CIO) {
    followRedirects = true
}

val json = Json { prettyPrint = true }

suspend fun listCards(kfreeKyword: String): List<CardId> {
    println("DEBUG: Searching for: $kfreeKyword")
    val response: HttpResponse = client.post("https://www.battlespirits.com/cardlist/index.php?search=true") {
        setBody(FormDataContent(Parameters.build {
            append("freewords", kfreeKyword)
            append("view_switch", "on") 
        }))
    }
    val html = response.bodyAsText()
    val idRegex = Regex("<span class=\"num\">\n(.*?)</span>")
    val ids = idRegex.findAll(html).map { it.groupValues[1].trim() }.toList()
    println("DEBUG: Found IDs: ${ids.size}")
    return ids
}

fun parseDetailIframe(html: String): Pair<List<String>, String> {
    val cleanHtml = html.replace("\n", "").replace("\r", "")
    val lvCostList = mutableListOf<String>()

    val lvItemRegex = Regex("<div class=\"bpCoreItem\\s*"(.*?)</div>")
    lvItemRegex.findAll(cleanHtml).forEach {
        val content = it.groupValues[1]
        val lvMatch = Regex("alt=\"LV(\\d+)\"").find(content)
        val bpMatch = Regex("class=\"bpCoreVal\">(\\d+)</span>").find(content)
        val coreMatch = Regex("class=\"bpCoreLevel\">\\s*(\\d+)</span>").find(content)

        if (lvMatch != null && bpMatch != null && coreMatch != null) {
            lvCostList.add("${lvMatch.groupValues[1]},${coreMatch.groupValues[1]},${bpMatch.groupValues[1]}")
        }
    }

    val effectBlockRegex = Regex("<dt class=\"detailColListTerm\">能力・効果</dt>\\s*<dd class=\"detailColListDescription[^\"]*">.*?</dd>")
    val effectBlockMatch = effectBlockRegex.find(cleanHtml)
    var effectText = ""
    if (effectBlockMatch != null) {
        val ddContentRegex = Regex("<dd[^>]*>(.*?)</dd>")
        val ddMatch = ddContentRegex.find(effectBlockMatch.value)
        if (ddMatch != null) {
             effectText = ddMatch.groupValues[1].replace(Regex("<img[^>]*alt=\"([^\"]+)"[^>]*>")) {
                "[${it.groupValues[1]}]"
            }
            .replace("<br>", "\n").replace("<br />", "\n")
            .replace(Regex("<[^>]*>"), "").trim()
        }
    }
    return Pair(lvCostList, effectText)
}

suspend fun Card.updateCache(cardId: CardId): Card {
    val cacheDir = File("cards")
    if (!cacheDir.exists()) cacheDir.mkdirs()
    
    val existingFile = cacheDir.listFiles { _, name -> name.startsWith("${cardId}_") || name.startsWith("$cardId.") }?.firstOrNull()
    if (existingFile != null) {
        println("DEBUG: Cache hit for $cardId")
        return try {
            json.decodeFromString(Card.serializer(), existingFile.readText())
        } catch (e: Exception) {
            fetchAndSave(cardId)
        }
    }
    return fetchAndSave(cardId)
}

suspend fun fetchAndSave(cardId: CardId): Card {
    println("DEBUG: Fetching web for $cardId")
    val listResponse: HttpResponse = client.post("https://www.battlespirits.com/cardlist/index.php?search=true") {
        setBody(FormDataContent(Parameters.build {
            append("freewords", cardId) 
            append("view_switch", "on") 
        }))
    }
    val listHtml = listResponse.bodyAsText()
    
    val nameRegex = Regex("<h3 class=\"name\">\n(.*?)</h3>")
    val nameMatch = nameRegex.find(listHtml)
    val name = nameMatch?.groupValues?.get(1)?.trim() ?: "Unknown"
    
    val attrMatch = Regex("<span class=\"attribute\">\n(.*?)</span>").find(listHtml)
    val attr = attrMatch?.groupValues?.get(1) ?: ""
    
    val systemMatch = Regex("<span class=\"system\">\n(.*?)</span>").find(listHtml)
    val family = systemMatch?.groupValues?.get(1)?.replace("・", ",") ?: ""
    
    val costMatch = Regex("<dd class=\"costVal\">\n(.*?)</dd>").find(listHtml)
    val cost = costMatch?.groupValues?.get(1) ?: ""

    val detailUrl = "https://www.battlespirits.com/cardlist/detail_iframe.php?card_no=$cardId&card_no2=$cardId"
    val detailResponse: HttpResponse = client.get(detailUrl)
    val detailHtml = detailResponse.bodyAsText()
    val (lvCost, effect) = parseDetailIframe(detailHtml)
    
    val categoryFullRegex = Regex("<dt class=\"detailColListTerm\">カテゴリー</dt>\\s*<dd[^>]*>(.*?)</dd>")
    val categoryFull = categoryFullRegex.find(detailHtml)?.groupValues?.get(1)?.trim() ?: "スピリット"
    val categoryShort = when {
        categoryFull.contains("スピリット") -> "S"
        categoryFull.contains("アルティメット") -> "U"
        categoryFull.contains("ブレイヴ") -> "B"
        categoryFull.contains("ネクサス") -> "N"
        categoryFull.contains("マジック") -> "M"
        else -> "O"
    }

    val newCard = Card(
        id = cardId,
        name = name,
        category = categoryShort,
        cost = cost,
        attr = attr,
        family = family,
        effect = effect,
        lvCost = lvCost
    )
    
    val safeName = name.replace("/", "_").replace(":", "：")
    val fileName = "cards/${cardId}_${safeName}_${categoryShort}_${attr}_${family}.json"
    File(fileName).writeText(json.encodeToString(Card.serializer(), newCard), StandardCharsets.UTF_8)
    println("DEBUG: Saved $fileName")
    
    return newCard
}

runBlocking {
    try {
        val searchWords = args.map { it.toString() }
        val keyword = if (searchWords.isNotEmpty()) searchWords.joinToString(" ") else "ノヴァ"
        
        println("DEBUG: Running search for $keyword")
        val ids = listCards(keyword)
        println("DEBUG: ID List: ${ids.take(3)}...")
        
        if (ids.isNotEmpty()) {
            val targetId = ids.first()
            val dummy = Card(targetId, "Dummy")
            val result = dummy.updateCache(targetId)
            println("DEBUG: Result: ${result.name}")
        }
    } catch (e: Exception) {
        e.printStackTrace()
    } finally {
        client.close()
        println("DEBUG: Done")
    }
}

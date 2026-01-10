@file:Repository("https://repo.maven.apache.org/maven2")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-io-core:0.8.2")
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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.serialization.json.*

fun parseDetailPage(html: String): Pair<List<String>, String> {
    val lvCostList = mutableListOf<String>()
    val cleanHtml = html.replace("\n", "").replace("\r", "")

    // Pattern: <div class="bpCoreItem ..."> ... alt="LV1" ... class="bpCoreVal">5000</span> ...
    val lvItemRegex = Regex("<div class=\"bpCoreItem\\s*\"(.*?)</div>")
    val lvItems = lvItemRegex.findAll(cleanHtml)

    lvItems.forEach {
        val content = it.groupValues[1]
        val lvMatch = Regex("alt=\"LV(\\d+)\"").find(content)
        val bpMatch = Regex("class=\"bpCoreVal\">(\\d+)</span>").find(content)
        val coreMatch = Regex("class=\"bpCoreLevel\">\\s*(\\d+)</span>").find(content)

        if (lvMatch != null && bpMatch != null && coreMatch != null) {
            lvCostList.add("${lvMatch.groupValues[1]},${coreMatch.groupValues[1]},${bpMatch.groupValues[1]}")
        }
    }

    // Pattern: <dt class="detailColListTerm">能力・効果</dt> ... <dd ...>(.*?)</dd>
    val effectBlockRegex =
        Regex("<dt class=\"detailColListTerm\">能力・効果</dt>\\s*<dd class=\"detailColListDescription[^\"]*\">.*?<\/dd>")
    val effectBlockMatch = effectBlockRegex.find(cleanHtml)
    var effectText = "取得失敗"
    if (effectBlockMatch != null && effectBlockMatch.groupValues.size > 1) {
        effectText = effectBlockMatch.groupValues[1].replace(Regex("""<img[^>]*alt=\"([^"]+)\"[^>]*>""")) {
            if (it.groupValues.size > 1) "[${it.groupValues[1]}]" else "[アイコン]"
        }
            .replace("<br>", "\n").replace("<br />", "\n")
            .replace(Regex("<[^>]*>"), "").trim()
    }
    return Pair(lvCostList, effectText)
}

fun runSearch() = runBlocking {
    val client = HttpClient(CIO) {
        followRedirects = true
    }

    val json = Json { prettyPrint = true }

    try {
        println("Fetching Card List using Ktor 3.0.3 & kotlinx.serialization (runtime)...")
        val response: HttpResponse = client.post("https://www.battlespirits.com/cardlist/index.php?search=true") {
            setBody(FormDataContent(Parameters.build {
                append("prodid", "")
                append("category[]", "スピリット")
                append("cost[min]", "3")
                append("cost[max]", "16")
                append("attribute[switch]", "OR")
                append("attribute[]", "赤")
                append("attribute[]", "紫")
                append("system[switch]", "AND")
                append("system[]", "超星")
                append("freewords", "ノヴァ")
                append("view_switch", "on")
            }))
        }

        val listHtml = response.bodyAsText()
        val cardBlockRegex = Regex("<li class=\"cardCol\\s+js-detail\">.*?<\/li>", RegexOption.DOT_MATCHES_ALL)
        val blocks = cardBlockRegex.findAll(listHtml)

        val cardsDir = File("cards")
        if (!cardsDir.exists()) cardsDir.mkdirs()

        blocks.forEach { block ->
            val html = block.groupValues[1]
            val id = Regex("<span class=\"num\">(.*?)</span>").find(html)?.groupValues?.get(1)?.trim() ?: return@forEach
            val name = Regex("<h3 class=\"name\">(.*?)</h3>").find(html)?.groupValues?.get(1)?.trim() ?: "Unknown"
            val cost = Regex("<dd class=\"costVal\">(.*?)</dd>").find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val attr = Regex("<span class=\"attribute\">(.*?)</span>").find(html)?.groupValues?.get(1) ?: "不明"
            val system = Regex("<span class=\"system\">(.*?)</span>").find(html)?.groupValues?.get(1) ?: ""

            val reductionHtml = Regex(
                "<dd class=\"alleviationVal\">(.*?)</dd>",
                RegexOption.DOT_MATCHES_ALL
            ).find(html)?.groupValues?.get(1) ?: ""
            val reductionMap = mutableMapOf<String, Int>()
            Regex("""alt="([^"]+)"""").findAll(reductionHtml).forEach {
                val key = if (it.groupValues[1] == "◇") "全" else it.groupValues[1]
                reductionMap[key] = reductionMap.getOrDefault(key, 0) + 1
            }
            val reductionStr = reductionMap.entries.joinToString("") { "${it.key}${it.value}" }

            val detailUrl = "https://www.battlespirits.com/cardlist/detail_iframe.php?card_no=$id&card_no2=$id"
            val detailResponse: HttpResponse = client.get(detailUrl)
            val detailHtml = detailResponse.bodyAsText()
            val (lvCost, effect) = parseDetailPage(detailHtml)

            val familyList = system.split("・").filter { it.isNotBlank() }

            val jsonObject = buildJsonObject {
                put("id", id)
                put("cardName", name)
                put("category", "S")
                put("attr", attr)
                put("cost", cost)
                put("symbol", "${attr.take(1)}1")
                put("reduction", reductionStr)
                putJsonArray("family") {
                    familyList.forEach { add(it) }
                }
                putJsonArray("lvCost") {
                    lvCost.forEach { add(it) }
                }
                put("effect", effect)
            }

            val jsonContent = json.encodeToString(JsonObject.serializer(), jsonObject)
            val fileName = "$id.${name.replace("/", "_")}.S.$attr.${familyList.joinToString(",")}.json"
            File(cardsDir, fileName).writeText(jsonContent, StandardCharsets.UTF_8)

            println("Processed: $id $name")
            delay(300)
        }
    } finally {
        client.close()
    }
}

runSearch()
println("Done.")
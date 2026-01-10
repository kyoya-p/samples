import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.net.URLEncoder
import java.io.File

fun encode(s: String): String = URLEncoder.encode(s, StandardCharsets.UTF_8.name())

fun fetchContent(urlStr: String, method: String = "GET", postData: ByteArray? = null): String {
    val url = URL(urlStr)
    val conn = url.openConnection() as HttpURLConnection
    try {
        conn.requestMethod = method
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
        if (method == "POST" && postData != null) {
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { it.write(postData) }
        }
        val inputStream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
        return inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    } finally {
        conn.disconnect()
    }
}

fun parseDetailPage(html: String): Pair<List<String>, String> {
    val lvCostList = mutableListOf<String>()
    val cleanHtml = html.replace("\n", "").replace("\r", "")
    
    val lvItemRegex = """<div class="bpCoreItem\s*">(.*?)</div>""".toRegex()
    val lvItems = lvItemRegex.findAll(cleanHtml)
    lvItems.forEach { 
        val content = it.groupValues[1]
        val lvMatch = """alt=\"LV(\d+)\"""".toRegex().find(content)
        val bpMatch = """class=\"bpCoreVal">(\d+)</span>""".toRegex().find(content)
        val coreMatch = """class=\"bpCoreLevel">\s*(\d+)</span>""".toRegex().find(content)
        if (lvMatch != null && bpMatch != null && coreMatch != null) {
            lvCostList.add("${lvMatch.groupValues[1]},${coreMatch.groupValues[1]},${bpMatch.groupValues[1]}")
        }
    }

    val effectBlockRegex = """<dt class="detailColListTerm">能力・効果</dt>\s*<dd class="detailColListDescription[^\"]*">.*?</dd>""".toRegex()
    val effectBlockMatch = effectBlockRegex.find(cleanHtml)
    var effectText = "取得失敗"
    if (effectBlockMatch != null) {
        effectText = effectBlockMatch.groupValues[1]
            .replace(Regex("<img[^>]*alt=\"([^\"]+)\"[^>]*>")) { "[${it.groupValues[1]}]" }
            .replace("<br>", "\n").replace("<br />", "\n")
            .replace(Regex("<[^>]*>"), "").trim()
    }
    return Pair(lvCostList, effectText)
}

println("Starting Battle Spirits Card Data Collector (StdLib)...")
val sb = StringBuilder()
sb.append("prodid=&category%5B%5D=").append(encode("スピリット"))
sb.append("&cost%5Bmin%5D=3&cost%5Bmax%5D=16&attribute%5Bswitch%5D=OR")
sb.append("&attribute%5B%5D=").append(encode("赤"))
sb.append("&attribute%5B%5D=").append(encode("紫"))
sb.append("&system%5Bswitch%5D=AND&system%5B%5D=").append(encode("超星"))
sb.append("&freewords=").append(encode("ノヴァ"))
sb.append("&view_switch=on")

val listHtml = fetchContent("https://www.battlespirits.com/cardlist/index.php?search=true", "POST", sb.toString().toByteArray())
val blocks = """<li class="cardCol\s+js-detail">(.*?)</li>""".toRegex(RegexOption.DOT_MATCHES_ALL).findAll(listHtml)

val cardsDir = File("cards")
if (!cardsDir.exists()) cardsDir.mkdirs()

blocks.forEach { block ->
    val html = block.groupValues[1]
    val id = """<span class="num">(.*?)</span>""" .toRegex().find(html)?.groupValues?.get(1)?.trim() ?: return@forEach
    val name = """<h3 class="name">(.*?)</h3>""" .toRegex().find(html)?.groupValues?.get(1)?.trim() ?: "Unknown"
    val cost = """<dd class="costVal">(.*?)</dd>""" .toRegex().find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    val attr = """<span class="attribute">(.*?)</span>""" .toRegex().find(html)?.groupValues?.get(1) ?: "不明"
    val system = """<span class="system">(.*?)</span>""" .toRegex().find(html)?.groupValues?.get(1) ?: ""
    
    val reductionHtml = """<dd class="alleviationVal">(.*?)</dd>""" .toRegex(RegexOption.DOT_MATCHES_ALL).find(html)?.groupValues?.get(1) ?: ""
    val reductionMap = mutableMapOf<String, Int>()
    """alt=\"([^\"]+)\"""".toRegex().findAll(reductionHtml).forEach { 
        val key = if (it.groupValues[1] == "◇") "全" else it.groupValues[1]
        reductionMap[key] = reductionMap.getOrDefault(key, 0) + 1
    }
    val reductionStr = reductionMap.entries.joinToString("") { "${it.key}${it.value}" }

    val detailHtml = fetchContent("https://www.battlespirits.com/cardlist/detail_iframe.php?card_no=$id&card_no2=$id")
    val (lvCost, effect) = parseDetailPage(detailHtml)

    val categoryCode = if (html.contains("スピリット")) "S" else "S" // Simplified
    val familyList = system.split("・").filter { it.isNotBlank() }

    val json = """
    {
      "id": "$id",
      "cardName": "$name",
      "category": "$categoryCode",
      "attr": "$attr",
      "cost": $cost,
      "symbol": "${attr.take(1)}1",
      "reduction": "$reductionStr",
      "family": ${familyList.map { "\"$it\"" }},
      "lvCost": ${lvCost.map { "\"$it\"" }},
      "effect": "${effect.replace("\"", "\\\"").replace("\n", "\\n")}"
    }
    """.trimIndent()

    val fileName = "$id.${name.replace("/", "_")}.$categoryCode.$attr.${familyList.joinToString(",")}.json"
    File(cardsDir, fileName).writeText(json, StandardCharsets.UTF_8)
    println("Processed: $id $name")
    Thread.sleep(300)
}
println("Done.")

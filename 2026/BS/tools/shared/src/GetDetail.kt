import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element

fun parseCardFace(root: Element, cardNo: String, sideName: String): CardFace {
    val rarity = root.select(".cardRarity").text().trim()
    val name = root.select(".cardName").text().trim()
    val costText = root.select(".textCost").text().trim()
    val cost = costText.toIntOrNull() ?: 0
    val reductionImgs = root.select(".cost img").map { img ->
        val src = img.attr("src")
        when {
            src.contains("cost_ruby") -> "赤"
            src.contains("cost_amethyst") -> "紫"
            src.contains("cost_emerald") -> "緑"
            src.contains("cost_diamond") -> "白"
            src.contains("cost_topaz") -> "黄"
            src.contains("cost_sapphire") -> "青"
            src.contains("cost_all") -> "全"
            src.contains("cost_god") -> "神"
            else -> img.attr("alt")
        }
    }
    val reductionSymbols =
        reductionImgs.groupingBy { it }.eachCount().entries.joinToString("") { "${it.key}${it.value}" }
    val category = root.select("dt:contains(カテゴリー) + dd").text().trim()
    val attributes = root.select(".attribute .attributeItem").joinToString("") { it.text().trim() }

    val systems = root.select("dt:contains(系統) + dd").first()?.ownText()?.trim()
        ?.split("・")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()

    val lvInfoList = mutableListOf<String>()
    val bpCoreItems = root.select(".bpCoreItem")

    if (bpCoreItems.isNotEmpty()) {
        bpCoreItems.forEach { item ->
            val lvImg = item.select("img.bpCoreImg").attr("alt")
            if (lvImg.isNotEmpty()) {
                val lv = lvImg.replace("LV", "").toIntOrNull() ?: 0
                val bp = item.select(".bpCoreVal").text().trim().toIntOrNull()
                val core = item.select(".bpCoreLevel").text().trim().toIntOrNull() ?: 0
                if (bp != null) lvInfoList.add("$lv,$core,$bp")
                else lvInfoList.add("$lv,$core")
            }
        }
    } else {
        // Fallback for Nexuses if they don't use .bpCoreItem
        val dts = root.select("dt")
        dts.forEach { dt ->
            val dtText = dt.text().trim()
            if (dtText.startsWith("Lv")) {
                val lv = dtText.replace("Lv", "").toIntOrNull() ?: 0
                val dd = dt.nextElementSibling()
                if (dd != null && dd.tagName() == "dd") {
                    val core = dd.text().replace("コア", "").trim().toIntOrNull() ?: 0
                    lvInfoList.add("$lv,$core")
                }
            }
        }
    }

    val effectElement = root.select(".detailColListDescription.wide").first()?.clone()
    effectElement?.select("img")?.forEach { img ->
        val alt = img.attr("alt")
        img.after("[" + alt + "]")
        img.remove()
    }
    val effect = effectElement?.let {
        val cleanHtml = it.html().replace("<br>", "\n").replace("<br />", "\n")
        Ksoup.parse(cleanHtml).text()
    }?.trim() ?: ""

    // Q&A取得
    val qaList = root.select(".detailColQAList").map { qaElem ->
        val qIdFull = qaElem.select(".detailColQANum").text().trim()
        val qId = qIdFull.split(" ").firstOrNull() ?: ""
        val qDate = qaElem.select(".detailColQANumDate").text().trim()
        val question = qaElem.select(".detailColQATerm").text().trim()
        val answer = qaElem.select(".detailColQADescription").text().trim()
        QA(id = qId, date = qDate, question = question, answer = answer)
    }

    // シンボル推定ロジック
    var symbols = ""
    if (category in listOf("スピリット", "アルティメット", "ネクサス", "契約スピリット", "契約アルティメット", "契約ネクサス", "創界神ネクサス", "契約創界神ネクサス")) {
        val hasDoubleInEffect = effect.contains("このカードはシンボル2つを持つ") || effect.contains("このスピリットはシンボル2つを持つ")
        val hasTripleInEffect = effect.contains("このカードはシンボル3つを持つ") || effect.contains("このスピリットはシンボル3つを持つ")
        val hasDoubleInQA = qaList.any { it.question.contains("シンボルが2つ") || it.answer.contains("シンボルが2つ") }
        val hasTripleInQA = qaList.any { it.question.contains("シンボルが3つ") || it.answer.contains("シンボルが3つ") }

        val baseSymbolCount = when {
            hasTripleInEffect || hasTripleInQA -> 3
            hasDoubleInEffect || hasDoubleInQA -> 2
            else -> 1
        }
        
        val attrList = attributes.toCharArray().map { it.toString() }.filter { it.isNotBlank() }
        
        symbols = if (attrList.isEmpty()) {
            "無$baseSymbolCount"
        } else if (attrList.size == 1) {
            "${attrList[0]}$baseSymbolCount"
        } else {
            attrList.joinToString("") { "${it}1" }
        }
    }

    val restrictionRaw = root.select(".detailColLimit .detailColLimitInner").text().trim()
    val restriction = when {
        restrictionRaw.contains("デッキに入れられません") -> "禁止"
        restrictionRaw.contains("1枚まで") -> "制限1"
        restrictionRaw.isNotEmpty() -> restrictionRaw
        else -> ""
    }

    val imageUrl = "https://www.battlespirits.com" + root.select(".cardImg img").attr("src")

    return CardFace(
        cardNo = cardNo,
        side = sideName,
        name = name,
        rarity = rarity,
        cost = cost,
        symbols = symbols,
        reductionSymbols = reductionSymbols,
        category = category,
        attributes = attributes,
        systems = systems,
        lvInfo = lvInfoList,
        effect = effect,
        restriction = restriction,
        imageUrl = imageUrl,
        qa = qaList
    )
}

fun parseCard(html: String): Card {
    val doc: Document = Ksoup.parse(html)
    val id = doc.select(".cardNum").first()?.ownText()?.trim() ?: ""

    val sideA = doc.select("#CardCol_A").firstOrNull()?.let { parseCardFace(it, id, "A") }
    val sideB = doc.select("#CardCol_B").firstOrNull()?.let { parseCardFace(it, id, "B") }
    val sideNo = doc.select(".detailBox").firstOrNull()?.let { parseCardFace(it, id, "") }

    val finalSideA = sideA ?: sideNo ?: throw IllegalArgumentException("Card face not found for ID: $id")

    return Card(id, finalSideA, sideB)
}

suspend fun bsDetail(client: HttpClient, cardId: String): Pair<Card, String> {
    val response = client.get("https://www.battlespirits.com/cardlist/detail_iframe.php") {
        parameter("card_no", cardId)
        header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
    }
    val body = response.bodyAsText()
    return parseCard(body) to body
}

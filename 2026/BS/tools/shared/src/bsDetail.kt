import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow

fun parseCardFace(root: Element, cardNo: String, sideName: String): CardFace {
    val rarity = root.select(".cardRarity").text().trim()
    val name = root.select(".cardName").text().trim()
    val costText = root.select(".textCost").text().trim()
    val cost = costText.toIntOrNull() ?: 0
    val reductionImgs = root.select(".cost img").map { it.attr("alt") }
    val reductionSymbols =
        reductionImgs.groupingBy { it }.eachCount().entries.joinToString("") { "${it.key}${it.value}" }
    val symbolImgs = root.select("dt:contains(シンボル) + dd img").map { it.attr("alt") } //todo
    val symbols = symbolImgs.groupingBy { it }.eachCount().entries.joinToString("") { "${it.key}${it.value}" } //todo
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
        imageUrl = imageUrl
    )
}

fun parseCard(html: String): List<Card> {
    val doc: Document = Ksoup.parse(html)
    val id = doc.select(".cardNum").first()?.ownText()?.trim() ?: ""

    val sideA = doc.select("#CardCol_A").firstOrNull()?.let { parseCardFace(it, id, "A") }
    val sideB = doc.select("#CardCol_B").firstOrNull()?.let { parseCardFace(it, id, "B") }
    val sideNo = doc.select(".detailBox").firstOrNull()?.let { parseCardFace(it, id, "") }

    val cards = mutableListOf<Card>()
    if (sideA != null) {
        cards.add(Card(id, sideA, sideB))
    } else if (sideNo != null) {
        cards.add(Card(id, sideNo, null))
    }

    return cards
}

suspend fun bsDetail(cardId: String): Flow<Card> = flow {
    val client = HttpClient()
    val response = client.get("https://www.battlespirits.com/cardlist/detail_iframe.php") {
        parameter("card_no", cardId)
        header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
    }
    val body = response.bodyAsText()
    client.close()
    parseCard(body).forEach { emit(it) }
}

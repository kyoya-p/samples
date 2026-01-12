@file:DependsOn("io.ktor:ktor-client-core-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.3.3")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
@file:DependsOn("com.fleeksoft.ksoup:ksoup-jvm:0.2.0")
@file:Import("./bsModel.main.kts")

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element

fun parseCardSide(root: Element, cardNo: String, sideName: String): Card {
    val rarity = root.select(".cardRarity").text().trim()
    val name = root.select(".cardName").text().trim()

    val costText = root.select(".textCost").text().trim()
    val cost = costText.toIntOrNull() ?: 0
    val reductionSymbols = root.select(".cost img").map { it.attr("alt") }

    val category = root.select("dt:contains(カテゴリー) + dd").text().trim()
    val attributes = root.select(".attribute .attributeItem").map { it.text().trim() }
    val systems = root.select("dt:contains(系統) + dd").text().split("・").map { it.trim() }.filter { it.isNotBlank() }

    val lvInfo = root.select(".bpCoreItem").mapNotNull { item ->
        val lvImg = item.select("img.bpCoreImg").attr("alt")
        if (lvImg.isEmpty()) return@mapNotNull null

        val lv = lvImg.replace("LV", "").toIntOrNull() ?: 0
        val bp = item.select(".bpCoreVal").text().trim().toIntOrNull()
        val core = item.select(".bpCoreLevel").text().trim().toIntOrNull() ?: 0
        LvInfo(lv, core, bp)
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

    return Card(
        cardNo = cardNo,
        side = sideName,
        name = name,
        rarity = rarity,
        cost = cost,
        reductionSymbols = reductionSymbols,
        attributes = attributes,
        category = category,
        systems = systems,
        lvInfo = lvInfo,
        effect = effect,
        imageUrl = imageUrl
    )
}

fun parseCard(html: String): List<Card> {
    val doc: Document = Ksoup.parse(html)
    val id = doc.select(".cardNum").first()?.ownText()?.trim() ?: ""

    val sideA = doc.select("#CardCol_A").firstOrNull()?.let { parseCardSide(it, id, "A") }
    val sideB = doc.select("#CardCol_B").firstOrNull()?.let { parseCardSide(it, id, "B") }
    val sideNo = doc.select(".detailBox").firstOrNull()?.let {parseCardSide(it, id, "") }

//    if (sideA != null) {
//        sides.add(parseCardSide(sideA, id, "A"))
//        if (sideB != null) {
//            sides.add(parseCardSide(sideB, id, "B"))
//        }
//    } else {
//        doc.select(".detailBox").first()?.let {
//            sides.add(parseCardSide(it, id, ""))
//        }
//    }

    return listOfNotNull(sideA, sideB, sideNo)
}

suspend fun bsDetail(cardId: String): List<Card> {
    val client = HttpClient(CIO)
    val response = client.get("https://www.battlespirits.com/cardlist/detail_iframe.php") {
        parameter("card_no", cardId)
        header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
    }
    val body = response.bodyAsText()
    client.close()
    return parseCard(body)
}

// スクリプトテスト用実行コード
if (args.getOrNull(0) == "detail") runBlocking {
    val id = args.getOrNull(1) ?: ""
    if (id.isEmpty()) {
        println("Usage: kotlin bsDetail.main.kts detail <CardID>")
    } else {
        bsDetail(id).forEach { println(it) }
    }
}
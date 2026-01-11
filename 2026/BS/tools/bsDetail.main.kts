@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("io.ktor:ktor-client-core-jvm:3.0.3")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.0.3")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
@file:DependsOn("com.fleeksoft.ksoup:ksoup-jvm:0.2.0")

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element
import kotlinx.serialization.Serializable

@Serializable
data class CardDetail(
    val id: String,
    val side: String, // ""(none),"A","B"
    val name: String,
    val rarity: String,
    val cost: Int?,
    val reductionSymbols: List<String>,
    val attributes: List<String>,
    val category: String,
    val systems: List<String>,
    val lvInfo: List<LvInfo>,
    val effect: String,
    val imageUrl: String
)

@Serializable
data class LvInfo(val level: Int, val core: Int, val bp: Int)

val client = HttpClient(CIO)

fun parseCard(html: String): CardDetail {
    val id = root.select(".cardNum").text().trim()
    val side = root.select(".cardSideAB").text().trim()
    val rarity = root.select(".cardRarity").text().trim()
    val name = root.select(".cardName").text().trim()
    val costText = root.select(".textCost").text().trim()
    val cost = costText.toIntOrNull()
    val reductionSymbols = root.select(".cost img").map { it.attr("alt") }
    val category = root.select("dt:contains(カテゴリー) + dd").text().trim()
    val attributes = root.select(".attribute .attributeItem").map { it.text().trim() }

    // Systems
    val systems = root.select("dt:contains(系統) + dd").text().split("・").filter { it.isNotBlank() }

    // Lv Info
    val lvInfo = root.select(".bpCoreItem").mapNotNull { item ->
        val lvImg = item.select("img.bpCoreImg").attr("alt")
        if (lvImg.isEmpty()) return@mapNotNull null

        val lv = lvImg.replace("LV", "").toIntOrNull() ?: 0
        val bp = item.select(".bpCoreVal").text().trim().toIntOrNull() ?: 0
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

    return CardDetail(
        id = id,
        side = side,
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

val targetCardNo = args.getOrElse(0) {
}
val targetCardSide = args.getOrNull(1) ?: ""

runBlocking {
    println("Fetching details for: $targetCardNo")
    val response = client.get("https://www.battlespirits.com/cardlist/detail_iframe.php") {
        parameter("card_no", targetCardNo)
        parameter("card_no2", targetCardNo)
        parameter("card_side", targetCardSide)
        header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"
        )
    }
    val detail = parseCard(response.bodyAsText())
    println(detail)
}

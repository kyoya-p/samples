import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun bsSearchMain(
    client: HttpClient,
    keywords: String,
    cardNo: String,
    costMin: Int,
    costMax: Int,
    attributes: List<String>,
    categories: List<String>,
    systems: List<String>,
    blockIcons: List<String>,
    attributeSwitch: String = "OR",
    systemSwitch: String = "OR",
): Flow<SearchCard> = flow {
    val response: HttpResponse = client.post("https://www.battlespirits.com/cardlist/index.php?search=true") {
        header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"
        )
        header("Referer", "https://www.battlespirits.com/cardlist/index.php?search=true")
        header("Origin", "https://www.battlespirits.com")
        contentType(ContentType.Application.FormUrlEncoded)

        val params = Parameters.build {
            append("prodid", cardNo)
            append("cost[min]", costMin.toString())
            append("cost[max]", costMax.toString())
            append("attribute[switch]", attributeSwitch)
            append("system[switch]", systemSwitch)
            append("view_switch", "on")
            append("freewords", keywords)

            categories.forEach { append("category[]", it) }
            attributes.forEach { append("attribute[]", it) }
            systems.forEach { append("system[]", it) }
            blockIcons.forEach { append("block_icon[]", it) }
        }
        setBody(params.formUrlEncode())
    }

    val body = response.bodyAsText()

    if (body.isNotEmpty()) {
        val doc: Document = Ksoup.parse(body)
        val count = doc.selectFirst(".js-countContents")?.text()?.trim() ?: ""
        val errorElement = doc.selectFirst(".errorCol.is-show .errorColheading")
        if (errorElement != null) return@flow println("Error: ${errorElement.text().trim()} (Count: $count)")
        if (count.isNotEmpty()) println("Found $count cards.")

        val cardElements = doc.select("li.cardCol.js-detail")

        cardElements.forEach { element ->
            val rawType = element.select(".type").text().trim()
            // Example: "赤 | スピリット 星竜・勇傑"
            val parts = rawType.split("|").map { it.trim() }
            val attribute = if (parts.size > 1) parts[0].split("・").joinToString("") else ""
            val typeAndSystem = if (parts.size > 1) parts[1] else parts[0]

            val typeParts = typeAndSystem.split(" ", limit = 2)
            val type = typeParts[0]
            val systems = if (typeParts.size > 1) typeParts[1].split("・").map { it.trim() }
                .filter { it.isNotBlank() } else emptyList()

            emit(
                SearchCard(
                    cardNo = element.select(".number .num").text().trim(),
                    rarity = element.select(".number .rarity").text().trim(),
                    name = element.select(".name").text().trim(),
                    cost = element.select(".costVal").text().trim(),
                    category = type,
                    attribute = attribute,
                    systems = systems,
                    imgUrl = "https://www.battlespirits.com${element.select(".thumbnail img").attr("data-src")}",
                )
            )
        }
    }
}
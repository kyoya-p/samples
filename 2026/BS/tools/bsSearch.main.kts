@file:DependsOn("io.ktor:ktor-client-core-jvm:3.3.3")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.3.3")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
@file:DependsOn("com.fleeksoft.ksoup:ksoup-jvm:0.2.0")
@file:Import("./bsModel.main.kts")

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable

val searchClient = HttpClient(CIO) {
    install(HttpTimeout) {
        requestTimeoutMillis = 60000
        connectTimeoutMillis = 60000
        socketTimeoutMillis = 60000
    }
}

//@Serializable
//data class SearchCard(
//    val cardNo: String,
//    val name: String,
//    val rarity: String,
//    val cost: String,
//    val type: String,
//    val imgUrl: String,
//)

// Main
runBlocking {
    bsSearchMain(
        keywords = args.joinToString(" "),
        cardNo = "",
        costMin = 0,
        costMax = 10,
        attr = "", // 全色の場合 "赤紫緑白黄青"
        category = listOf(),
        system = listOf(),
    ).collectIndexed { index, card ->
        println("$index: $card")
    }
}
suspend fun bsSearchMain(
    keywords: String,
    cardNo: String,
    costMin: Int,
    costMax: Int,
    attr: String,
    category: List<String>,
    system: List<String>,
): Flow<SearchCard> = flow {
    val response: HttpResponse = searchClient.post("https://www.battlespirits.com/cardlist/index.php?search=true") {
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
            append("attribute[switch]", "OR")
            append("system[switch]", "OR")
            append("view_switch", "on")
            append("freewords", keywords)
            if (category.isNotEmpty()) {
                append("category[]", category.joinToString(" "))
            }
        }
        setBody(params.formUrlEncode())
    }

    val body = response.bodyAsText()

    if (body.isNotEmpty()) {
        val doc: Document = Ksoup.parse(body)
        val cardElements = doc.select("li.cardCol.js-detail")

        cardElements.forEach { element ->
            emit(
                SearchCard(
                    cardNo = element.select(".number .num").text().trim(),
                    rarity = element.select(".number .rarity").text().trim(),
                    name = element.select(".name").text().trim(),
                    cost = element.select(".costVal").text().trim(),
                    type = element.select(".type").text().trim(),
                    imgUrl = "https://www.battlespirits.com${element.select(".thumbnail img").attr("data-src")}",
                )
            )
        }
    }
}

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable

val client = HttpClient(CIO) {
    install(HttpTimeout) {
        requestTimeoutMillis = 60000
        connectTimeoutMillis = 60000
        socketTimeoutMillis = 60000
    }
}

@Serializable
data class Card(
    val id: String,
    val name: String,
    val rarity: String,
    val cost: String,
    val type: String,
    val imgUrl: String,
)

// Main
runBlocking {
    bsSearchMain().collectIndexed { index, card ->
        println("$index: $card")
    }
}
suspend fun bsSearchMain(
    freeWord: String,
    cardNo: String,
    constMin: Int,
    constMax: Int,
    attr: String,
    category: List<String>,
    system: List<String>,
): Flow<Card> = flow {
    println("Searching for cards...")
    val response: HttpResponse = client.post("https://www.battlespirits.com/cardlist/index.php?search=true") {
        header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"
        )
        header("Referer", "https://www.battlespirits.com/cardlist/index.php?search=true")
        header("Origin", "https://www.battlespirits.com")
        contentType(ContentType.Application.FormUrlEncoded)

        val params = Parameters.build {
            append("prodid", "")
            append("cost[min]", "0")
            append("cost[max]", "30")
            append("attribute[switch]", "OR")
            append("system[switch]", "OR")
            append("view_switch", "on")
            append("freewords", "新しき世界")
            append("category[]", "スピリット")
        }
        setBody(params.formUrlEncode())
    }

    println("Response Status: ${response.status}")
    val body = response.bodyAsText()

    if (body.isNotEmpty()) {
        println("Parsing HTML with Ksoup...")
        val doc: Document = Ksoup.parse(body)

        val cardElements = doc.select("li.cardCol.js-detail")
        println("Found ${cardElements.size} cards.")
        println("---------------------------------------------------")

        cardElements.forEachIndexed { index, element ->
            val card = Card(
                id = element.select(".number .num").text(),
                rarity = element.select(".number .rarity").text(),
                name = element.select(".name").text(),
                cost = element.select(".costVal").text(),
                type = element.select(".type").text(),
                imgUrl = "https://www.battlespirits.com${element.select(".thumbnail img").attr("data-src")}",
            )
            emit(card)
        }
    }
}
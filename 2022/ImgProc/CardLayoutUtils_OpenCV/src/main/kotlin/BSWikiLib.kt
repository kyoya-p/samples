import io.ktor.client.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import org.jsoup.Jsoup

@Serializable
data class BSCard(
    val id: String? = null,
    val name: String? = null,
    val image: String? = null,
) {
    @JvmInline
    value class BS属性(val v: Int) {
    }
}

// Jsoup Selector
// https://www.javadoc.io/doc/org.jsoup/jsoup/1.2.3/org/jsoup/select/Selector.html

val defaultHttpClient = HttpClient {
    install(HttpCache)
}

suspend fun getBSCard(name: String): BSCard {
    val r = defaultHttpClient.get("https://batspi.com/index.php?$name").bodyAsText()
    //TODO キャッシュ
    val dom = Jsoup.parseBodyFragment(r)

    return BSCard(
        id = dom.selectFirst("*[class=cardno]")?.text(),
        name = dom.selectFirst("*[class=title]")?.text(),
        image = dom.selectFirst("*[class=ppy-stage]")?.attr("style")
    )
}

suspend fun cardSearch(id: String = "", 属性: Set<Int> = setOf()) = channelFlow {
    fun <T> Set<T>.joincr() = joinToString("\r")
    val wpara =
        "$id,2,,2,,2,0,,,,2,,,2,0,,0,${属性.joincr()},,,0,,,,,2,0,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,2,,2,,,,0,,1,,1,,1"
    val r = defaultHttpClient.get("https://batspi.com/index.php") {
        url {
            parameters.appendMissing("カード検索", listOf())
            parameters.append("wpara", wpara)
        }
    }
    println("URL:${r.request.url}")
    val body = Jsoup.parseBodyFragment(r.bodyAsText())
    body.select("*[id=dvCardList1] > table").forEach { cardTable ->
        val imgTd = cardTable.selectFirst("tbody > tr > td:eq(0)")!!
        val infoTd = cardTable.selectFirst("tbody > tr > td:eq(1)")!!
        //println(infoTd.selectFirst("table > tbody > tr:eq(1) > td > table > tbody > tr")?.outerHtml())
        val card = BSCard(
            image = imgTd.selectFirst("a")?.attr("href"),
            id = infoTd.selectFirst("table > tbody > tr:eq(1) > td > table > tbody > tr > td:eq(1)")?.text()
                ?.split(" ")?.first(),
            name = infoTd.selectFirst("table > tbody > tr:eq(1) > td > table > tbody > tr > td:eq(3)")?.text()
        )
        trySend(card)
    }
}

fun main() = runBlocking {
    //val r = getBSCard("幻の果物人ポポ")
    cardSearch(
        //id = "BS02-X08",
        属性 = setOf(1),
    ).collectIndexed { i, card ->
        println("$i: $card")
    }
}

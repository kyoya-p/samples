import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

suspend fun bsSearchMain(
    client: HttpClient,
    keywords: String,
    cardNo: String,
    costMin: Int,
    costMax: Int,
    attr: String,
    category: List<String>,
    system: List<String>,
) {
    println("L1")
    val response: HttpResponse = client.post("https://www.battlespirits.com/cardlist/index.php?search=true") {
        println("L2")
        header(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36"
        )
        header("Referer", "https://www.battlespirits.com/cardlist/index.php?search=true")
        header("Origin", "https://www.battlespirits.com")
        contentType(ContentType.Application.FormUrlEncoded)

        println("L3")
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
        println("L4")
        setBody(params.formUrlEncode())
    }

    println("L5")
    println("Response status: ${response.status}")

    val body = response.bodyAsText()
    println("L6")
    println(body)
}
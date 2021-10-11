import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*

fun main() {

    val client = HttpClient(CIO)

    client.get("http://")
}
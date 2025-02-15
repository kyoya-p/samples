import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class CatFact(val text: String)

suspend fun appMain() {
    val client = HttpClient(CIO) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    val res = client.get("http://cat-fact.herokuapp.com/facts").body<List<CatFact>>()
    res.forEachIndexed { i,f->println("$i: ${f.text}") }
}

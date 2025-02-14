import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.milliseconds


@Serializable
data class IP(val ip: String, val location: Location)

@Serializable
data class Location(val country: String, val city: String)

val client = HttpClient(CIO) { install(ContentNegotiation) { json() } }
suspend fun appMain() = runCatching {
    val client = HttpClient(CIO) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } }
    val res = client.get("https://api.ipapi.is/").body<IP>()
    println(res)
}.onFailure { it.printStackTrace() }


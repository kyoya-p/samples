import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json

actual fun getEnv(key: String) = System.getenv(key) !!
actual val client = HttpClient { install(ContentNegotiation) { json() } }

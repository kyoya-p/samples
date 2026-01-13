import io.ktor.client.*
import kotlinx.coroutines.runBlocking
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.utils.io.core.use
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun main() = runBlocking {
    withContext(Dispatchers.Default) {
        HttpClient {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 15000
                connectTimeoutMillis = 10000
                socketTimeoutMillis = 15000
            }
        }.use { client ->
            bsSearchMain(
                client = client,
                keywords = "キズナ",
                cardNo = "",
                costMin = 0,
                costMax = 30,
                attr = "",
                category = listOf(),
                system = listOf()
            )
        }
    }
}
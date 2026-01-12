import io.ktor.client.HttpClient

actual fun getEnv(key: String) = System.getenv(key) ?: ""
actual fun createClient(): HttpClient = HttpClient()

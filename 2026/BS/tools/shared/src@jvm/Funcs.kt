import io.ktor.client.HttpClient

actual fun getEnv(key: String) = System.getenv(key) ?: ""
actual val client = HttpClient()

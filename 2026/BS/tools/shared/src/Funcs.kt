import io.ktor.client.HttpClient

expect fun getEnv(key: String): String
expect val client: HttpClient
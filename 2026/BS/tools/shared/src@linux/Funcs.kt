import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import libcurl.curl_getenv

@OptIn(ExperimentalForeignApi::class)
actual fun getEnv(key: String): String = curl_getenv(key)?.toKString() ?: ""
actual val client = HttpClient(Curl) { install(ContentNegotiation) { json() } }

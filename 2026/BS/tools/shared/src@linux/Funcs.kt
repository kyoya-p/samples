import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.serialization.kotlinx.json.json
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import libcurl.curl_getenv

@OptIn(ExperimentalForeignApi::class)
actual fun getEnv(key: String): String = curl_getenv(key)?.toKString() ?: ""
actual fun createClient() = HttpClient(Curl)
@OptIn(ExperimentalForeignApi::class)
actual fun getCurrentTimestamp(): String = platform.posix.time(null).toString()

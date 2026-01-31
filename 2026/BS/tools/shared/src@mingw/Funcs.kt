import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
actual fun getEnv(key: String): String = getenv(key)?.toKString() ?: ""

actual fun createClient() = HttpClient(Curl) { engine { sslVerify = false } }

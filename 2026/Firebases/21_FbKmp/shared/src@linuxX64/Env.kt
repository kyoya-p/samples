import kotlinx.cinterop.*
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
actual fun getEnv(key: String): String? {
    return getenv(key)?.toKString()
}
import kotlinx.cinterop.*
import platform.posix.*

actual fun readFile() {
    val fd: Int = open("README.md", O_RDONLY)
    val buf = ByteArray(2048)
    buf.usePinned {
        read(fd, buf as CValuesRef<CPointed>, 256)
    }
    val contents = buf.toKString()
    println("file: $contents")
}

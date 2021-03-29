import kotlinx.cinterop.*
import platform.posix.*

actual fun readFile_SAMPLE() {
    val fd: Int = open("README.md", O_RDONLY)

    val buf = nativeHeap.allocArray<ByteVar>(2048)
    buf.usePinned {
        read(fd, buf, 2048)
    }
    val contents = buf.toKString()
    nativeHeap.free(buf)
    println("file: $contents")
}

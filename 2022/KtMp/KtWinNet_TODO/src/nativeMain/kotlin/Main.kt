import kotlinx.cinterop.*
import platform.posix.WSADATA
import platform.windows.*

fun main() {
    println("Hello, Kotlin/Native!")
    ip()
}

fun ip() {
    var wsaData: WSADATA? = null
    var iResult = WSAStartup(0x0202.toUShort(), wsaData?.ptr)
    if (iResult != 0) {
        println("WSAStartup failed: $iResult")
    }
}

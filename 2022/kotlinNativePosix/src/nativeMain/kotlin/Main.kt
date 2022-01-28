import platform.posix.*
import kotlinx.cinterop.*

fun main() = memScoped {
    val endpoint = "127.0.0.1"
    val port = 8080
    //var socketDescriptor = 0
    val serverAddr = alloc<sockaddr_in>()
    val socketDescriptor = socket(AF_INET, SOCK_STREAM, 0)
    with(serverAddr) {
        memset(this.ptr, 0, sockaddr_in.size.convert())
        sin_family = AF_INET.convert()
        sin_addr.S_un.S_addr = inet_addr(endpoint)
        sin_port = ((port shr 8) or ((port and 0xff) shl 8)).toUShort()
    }
    bind(socketDescriptor, serverAddr.ptr.reinterpret(), sockaddr_in.size.convert())
    println("sd=$socketDescriptor")
}

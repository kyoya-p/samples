import platform.posix.*
import kotlinx.cinterop.*

fun main() = memScoped {
    val endpoint = "127.0.0.1"
    val port = 8080
    val socketDescriptor = socket(AF_INET, SOCK_STREAM, 0)
    val serverAddr = alloc<sockaddr_in>().apply {
        // memset(this.ptr, 0, sockaddr_in.size.convert())
        memset(this.ptr, 0, sizeOf<sockaddr_in>().convert())
        sin_family = AF_INET.convert()
        sin_addr.S_un.S_addr = inet_addr(endpoint)
        sin_port = ((port shr 8) or ((port and 0xff) shl 8)).toUShort()
    }
    //bind(socketDescriptor, serverAddr.ptr.reinterpret(), sockaddr_in.size.convert())
    bind(socketDescriptor, serverAddr.ptr.reinterpret(), sizeOf<sockaddr_in>().convert())
    listen(socketDescriptor, 2)
    //val buf = CValues
    //recv(socketDescriptor, buf,buf.size)
    println("sd=$socketDescriptor")
}

import platform.posix.*
import kotlinx.cinterop.*

// https://gist.github.com/kavanmevada/20458c1e99f6d750bf122c3f975d0396
fun main() = memScoped {
    println("L0")
    val endpoint = "127.0.0.1"
    val port = 8080
    val socketDescriptor = socket(AF_INET, SOCK_STREAM, 0)
    println("L1")
    val serverAddr = alloc<sockaddr_in>().apply {
        // memset(this.ptr, 0, sockaddr_in.size.convert())
        memset(this.ptr, 0, sizeOf<sockaddr_in>().convert())
        sin_family = AF_INET.convert()
        sin_addr.S_un.S_addr = inet_addr(endpoint)
        sin_port = ((port shr 8) or ((port and 0xff) shl 8)).toUShort()
    }
    println("L2")

    //bind(socketDescriptor, serverAddr.ptr.reinterpret(), sockaddr_in.size.convert())
    bind(socketDescriptor, serverAddr.ptr.reinterpret(), sizeOf<sockaddr_in>().convert())
    println("L3")
    listen(socketDescriptor, 2)
    println("L4")
    //val buf = CValues
    //recv(socketDescriptor, buf,buf.size)

    println("sd=$socketDescriptor")
    println("L3")
    val newSocket = accept(socketDescriptor, null, null)
    if (newSocket == (-1).toULong()) error { println("ERROR: Obtaining new Socket Despcritor. (errno = $errno)") }
    else println("[Server] Server has got connected from ${serverAddr.getConnectedAddress()}.")

}

fun sockaddr_in.getConnectedAddress() = inet_ntoa(sin_addr.readValue())?.toKString()

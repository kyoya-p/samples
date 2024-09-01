import platform.posix.*
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
fun main() = memScoped {
    println("L0")
    val endpoint = "127.0.0.1"
    val port = 8080
    val socketDescriptor = socket(AF_INET, SOCK_STREAM, 0)
    println("L1")
    val serverAdr = alloc<sockaddr_in>().apply {
        memset(this.ptr, 0, sizeOf<sockaddr_in>().convert())
        sin_family = AF_INET.convert()
        sin_addr.S_un.S_addr = inet_addr(endpoint)
        sin_port = ((port shr 8) or ((port and 0xff) shl 8)).toUShort()
    }
    println("L2")

    bind(socketDescriptor, serverAdr.ptr.reinterpret(), sizeOf<sockaddr_in>().convert())
    println("L3")
    listen(socketDescriptor, 2)
    println("L4")
    //val buf = CValues
    //recv(socketDescriptor, buf,buf.size)

    println("sd=$socketDescriptor")
    println("L3")
    val newSocket = accept(socketDescriptor, null, null)
    if (newSocket == (-1).toULong()) error { println("ERROR: Obtaining new Socket Despcritor. (errNo = $errno)") }
    else println("[Server] Server has got connected from ${serverAdr.getConnectedAddress()}.")

}

@OptIn(ExperimentalForeignApi::class)
fun sockaddr_in.getConnectedAddress() = inet_ntoa(sin_addr.readValue())?.toKString()

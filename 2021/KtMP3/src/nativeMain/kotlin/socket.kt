import kotlinx.cinterop.*
//import platform.posix.* // https://kotlinlang.org/docs/native-platform-libs.html#posix-bindings
import platform.windows.*

/*

https://gist.github.com/kavanmevada/20458c1e99f6d750bf122c3f975d0396

 */

fun x() {
    val sock =
        socket(AF_INET, SOCK_STREAM, 0 /*unused*/) // https://linuxjm.osdn.jp/html/LDP_man-pages/man2/socket.2.html
    if (sock.toInt() == -1) {
        println("Error: Create socket.")
        return
    }
    
    closesocket(sock)
}


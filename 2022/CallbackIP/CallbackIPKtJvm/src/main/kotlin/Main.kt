import java.net.InetAddress
import java.net.NetworkInterface.getNetworkInterfaces
import java.net.Socket

fun main(args: Array<String>) {
    val dstAdr = InetAddress.getByName(args.getOrNull(0) ?: throw Exception("Error: Params"))
    val dstPort = (args.getOrNull(1) ?: throw Exception("Error: Params")).toInt()

    val localAdr = getLocalAdr1(dstAdr, dstPort)
    //val localAdr = getLocalAdr2(dstAdr, dstPort)
    println(localAdr?.hostAddress)
}

// すべてのlocalIPで接続できるかテスト
fun getLocalAdr1(dstAdr: InetAddress, dstPort: Int) = runCatching {
    val socket = Socket(dstAdr, dstPort, null, 0)
    val localAdr = socket.localAddress
    socket.close()
    localAdr
}.getOrNull()

// すべてのlocalIPで接続できるかテスト
fun getLocalAdr2(dstAdr: InetAddress, dstPort: Int): InetAddress? {
    for (ni in getNetworkInterfaces()) {
        for (srcAdr in ni.inetAddresses) {
            val result = runCatching {
                print("ni[${ni.name}] src:${srcAdr.hostAddress} ➔ ")
                val socket = Socket(dstAdr, dstPort, srcAdr, 0)
                print("dst:${socket.inetAddress.hostAddress} : ")
                socket.close()
                srcAdr
            }.getOrNull()
            println("${result?.hostAddress}")
            return result
        }
    }
    return null
}

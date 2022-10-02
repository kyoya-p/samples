import java.net.InetAddress
import java.net.NetworkInterface.getNetworkInterfaces
import java.net.Socket

fun main(args: Array<String>) {
    val dstAdr = InetAddress.getByName(args.getOrNull(0) ?: throw Exception("Error: Params"))
    val dstPort = args.getOrNull(1)?.toInt() ?: throw Exception("Error: Params")

    val localAdr = getLocalAdr_Sample1(dstAdr, dstPort)
//    val localAdr = getLocalAdr_Sample2(dstAdr, dstPort)
    println(localAdr?.hostAddress)
}

// Sample1. 接続済SocketのlocalAddressを取得 //これが簡単
fun getLocalAdr_Sample1(dstAdr: InetAddress, dstPort: Int) = runCatching {
    val socket = Socket(dstAdr, dstPort, null/*指定しない*/, 0)
    val localAdr = socket.localAddress
    socket.close()
    localAdr
}.getOrNull()

// Sample2. すべてのlocalIPで接続できるかテスト
fun getLocalAdr_Sample2(dstAdr: InetAddress, dstPort: Int): InetAddress? {
    for (ni in getNetworkInterfaces()) {
        for (srcAdr in ni.inetAddresses) {
           runCatching { //例外でたら次トライ
                print("ni.name=${ni.name} src:${srcAdr.hostAddress} ➔ ")
                val socket = Socket(dstAdr, dstPort, srcAdr, 0)
                println("dst:${socket.inetAddress.hostAddress}")
                socket.close()
                if(srcAdr!=null)return srcAdr
            }.onFailure { println("Exception") }
        }
    }
    return null
}

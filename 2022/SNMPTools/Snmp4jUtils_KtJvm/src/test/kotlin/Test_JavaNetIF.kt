import org.junit.jupiter.api.Test
import java.net.Inet6Address
import java.net.NetworkInterface


class Test_JavaNetIF {
    @Test
    fun iflist() {
        fun Boolean.t(t: String, f: String = "") = if (this) t else f

        for (ni in NetworkInterface.getNetworkInterfaces()) {
            for (a in ni.inetAddresses) {
                val ipv = when (a) {
                    is Inet6Address -> "v6"
                    else -> "v4"
                }

                println("%d-IA: %s%s%s%s %s %s".format(ni.index,
                    ipv,
                    a.isMulticastAddress.t("-MC"),
                    a.isLoopbackAddress.t("-LP"),
                    a.isLinkLocalAddress.t("-LL"),
                    a.hostName,
                    a.address.joinToString(".") { "%d".format(it.toUByte().toInt()) })
                )
            }
            for (a in ni.interfaceAddresses) {
                println("%d-IFA: %s %s".format(
                    ni.index,
                    a.address,
                    a.broadcast,
                ))
            }
        }
    }
}


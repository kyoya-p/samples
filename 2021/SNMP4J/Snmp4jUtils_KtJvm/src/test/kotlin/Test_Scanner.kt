@file:Suppress("ClassName")

import jp.`live-on`.shokkaa.scrambledIpV4AddressSequence
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.net.InetAddress

class Test_scrambledIpV4AddressFlow {
    @Test
    fun test1_1() = runBlocking {
        val adr = InetAddress.getByName("1.2.3.4")
        val res = scrambledIpV4AddressSequence(adr, 8).toList()
        assert(res[0].hostAddress.apply(::println) == "1.2.3.0")
        assert(res[1].hostAddress.apply(::println) == "1.2.3.128")
        assert(res[2].hostAddress.apply(::println) == "1.2.3.64")
        assert(res[254].hostAddress.apply(::println) == "1.2.3.127")
        assert(res[255].hostAddress.apply(::println) == "1.2.3.255")
    }

    @Test
    fun test1_2() = runBlocking {
        val adr = InetAddress.getByName("1.2.3.4")
        val res = scrambledIpV4AddressSequence(adr, 0).toList()
        assert(res.size == 1)
        assert(res[0].hostAddress.apply(::println) == "1.2.3.4")
    }

    @Test
    fun test1_3() = runBlocking {
        val adr = InetAddress.getByName("1.2.3.4")
        val res = scrambledIpV4AddressSequence(adr, 1).toList()
        assert(res.size == 2)
        assert(res[0].hostAddress.apply(::println) == "1.2.3.4")
        assert(res[1].hostAddress.apply(::println) == "1.2.3.5")
    }

}
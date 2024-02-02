@file:Suppress("ClassName")

import jp.wjg.shokkaa.snmp4jutils.async.createDefaultSenderSnmpAsync
import jp.wjg.shokkaa.snmp4jutils.scanFlow
import jp.wjg.shokkaa.snmp4jutils.scrambledIpV4AddressSequence
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.net.InetAddress

class ScannerTest {
    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun test1_scrambledIpV4AddressFlow_1() = runBlocking {
        val adr = InetAddress.getByName("1.2.3.4")
        val res = scrambledIpV4AddressSequence(adr, 8).toList()
        assert(res[0].second.hostAddress.apply(::println) == "1.2.3.0")
        assert(res[1].second.hostAddress.apply(::println) == "1.2.3.128")
        assert(res[2].second.hostAddress.apply(::println) == "1.2.3.64")
        assert(res[254].second.hostAddress.apply(::println) == "1.2.3.127")
        assert(res[255].second.hostAddress.apply(::println) == "1.2.3.255")
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun test1_scrambledIpV4AddressFlow_2() = runBlocking {
        val adr = InetAddress.getByName("1.2.3.4")
        val res = scrambledIpV4AddressSequence(adr, 0).toList()
        assert(res.size == 1)
        assert(res[0].second.hostAddress.apply(::println) == "1.2.3.4")
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun test1_scrambledIpV4AddressFlow_3() = runBlocking {
        val adr = InetAddress.getByName("1.2.3.4")
        val res = scrambledIpV4AddressSequence(adr, 1).toList()
        assert(res.size == 2)
        assert(res[0].second.hostAddress.apply(::println) == "1.2.3.4")
        assert(res[1].second.hostAddress.apply(::println) == "1.2.3.5")
    }

    @Test
    fun scanFlow_Test(): Unit = runTest {
        createDefaultSenderSnmpAsync().use { snmpAsync ->
            snmpAsync.scanFlow( "10.0.0.1","10.31.255.255").collect { res ->
                println("Res: ${res.peerAddress}: ${res.response.variableBindings}")
            }
        }
    }

    @Test
    fun RangeSetTest() {
    }
}

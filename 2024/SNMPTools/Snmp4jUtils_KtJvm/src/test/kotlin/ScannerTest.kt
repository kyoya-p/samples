@file:Suppress("ClassName")

import jp.wjg.shokkaa.snmp4jutils.ULongRangeSet
import jp.wjg.shokkaa.snmp4jutils.async.createDefaultSenderSnmpAsync
import jp.wjg.shokkaa.snmp4jutils.scanFlow
import jp.wjg.shokkaa.snmp4jutils.scrambledIpV4AddressSequence
import jp.wjg.shokkaa.snmp4jutils.toIPv4ULong
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.net.InetAddress
import kotlin.time.Duration.Companion.seconds

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
    fun scanFlow_Test(): Unit = runTest(timeout = 10.seconds) {
        val range =
            InetAddress.getByName("192.168.0.1").toIpV4ULong()..InetAddress.getByName("192.168.255.254").toIpV4ULong()
        val totalLength = ULongRangeSet(range).map { it.endInclusive - it.start + 1UL }.sum()
        var nEv = 0UL

        createDefaultSenderSnmpAsync().use { snmpAsync ->
            snmpAsync.scanFlow(range) { timeoutEvent = true }.collect { res ->
                println("Res[${++nEv}]: ${res.peerAddress}: ${res.response?.variableBindings}")
            }
        }
        assert(nEv == totalLength)
    }
}

@file:Suppress("ClassName")

import com.sun.jna.platform.win32.COM.Dispatch
import jp.wjg.shokkaa.snmp4jutils.async.*
import jp.wjg.shokkaa.snmp4jutils.ipV4AddressRangeFlow
import jp.wjg.shokkaa.snmp4jutils.ipV4AddressRangeSequence
import jp.wjg.shokkaa.snmp4jutils.scrambledIpV4AddressSequence
import jp.wjg.shokkaa.snmp4jutils.toIPv4ULong
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import java.net.InetAddress

class Test_Scanner {
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
    fun test_ipV4AddressRangeSequence(): Unit = runBlocking(Dispatchers.Default) {
        val snmp = defaultSenderSnmp
        val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID(".1"))))
        val startAdr = snmp.getInetAddressByName("10.36.102.1")
        val endAdr = snmp.getInetAddressByName("10.36.102.254")
        ipV4AddressRangeFlow(startAdr, endAdr).buffer(50)
            .map { ip ->
                pdu.requestID = Integer32(ip.toIPv4ULong().toInt())
                CommunityTarget(snmp.getUdpAddress(ip, 161), OctetString("public"))
            }
            .map { target ->
                snmp.sendAsync(pdu, target)
//                target
            }
            .collect { res ->
//                print("${res}:\n")
                print("${res.peerAddress}: ${res.response?.variableBindings}\r")
            }
    }
}

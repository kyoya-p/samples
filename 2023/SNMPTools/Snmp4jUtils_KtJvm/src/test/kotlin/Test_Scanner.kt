@file:Suppress("ClassName")

import jp.wjg.shokkaa.snmp4jutils.async.createDefaultSenderSnmpAsync
import jp.wjg.shokkaa.snmp4jutils.async.getInetAddressByName
import jp.wjg.shokkaa.snmp4jutils.async.getUdpAddress
import jp.wjg.shokkaa.snmp4jutils.async.sendAsync
import jp.wjg.shokkaa.snmp4jutils.ipV4AddressRangeFlow
import jp.wjg.shokkaa.snmp4jutils.scanFlow
import jp.wjg.shokkaa.snmp4jutils.scrambledIpV4AddressSequence
import jp.wjg.shokkaa.snmp4jutils.toIPv4ULong
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.smi.Integer32
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.VariableBinding
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
    fun test_ipV4AddressRange(): Unit = runBlocking {
        val snmp = createDefaultSenderSnmpAsync()
//        val startAdr = snmp.getInetAddressByName("1.0.0.1")
        val startAdr = snmp.getInetAddressByName("10.36.102.1")
//        val endAdr = snmp.getInetAddressByName("1.3.255.254")
        val endAdr = snmp.getInetAddressByName("10.36.102.255")
        var c = 0
        ipV4AddressRangeFlow(startAdr, endAdr).withIndex().map { (i, ip) ->
            print("Req $i [${++c}]:\r")
            async {
                runCatching {
                    val target = CommunityTarget(snmp.getUdpAddress(ip, 161), OctetString("public")).apply {
                        timeout = 2500
                        retries = 1
                    }
                    val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID(".1"))))
                    pdu.requestID = Integer32(ip.toIPv4ULong().toInt())
                    snmp.sendAsync(pdu, target).takeIf { it.response != null }?.let { res ->
                        println("Res:$i [$c] ip=$ip ${res.peerAddress}: ${res.response.variableBindings}")
                    }
                }.onFailure { it.printStackTrace() }
                c--
            }
        }.toList().awaitAll()
        println(c)
        snmp.close()
    }

    @Test
    fun scanFlow_Test(): Unit = runTest {
        createDefaultSenderSnmpAsync().use { snmpAsync ->
            val startAdr = snmpAsync.getInetAddressByName("10.0.0.1")
            val endAdr = snmpAsync.getInetAddressByName("10.0.0.254")
            snmpAsync.scanFlow(startAdr, endAdr).collect { res ->
                println("Res: ${res.peerAddress}: ${res.response.variableBindings}")
            }
        }
    }
}

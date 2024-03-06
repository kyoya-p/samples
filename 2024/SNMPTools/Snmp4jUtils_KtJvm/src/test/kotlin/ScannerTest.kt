@file:Suppress("ClassName")

import jp.wjg.shokkaa.snmp4jutils.DefaultSnmpScanRequest
import jp.wjg.shokkaa.snmp4jutils.async.Response
import jp.wjg.shokkaa.snmp4jutils.async.createDefaultSenderSnmpAsync
import jp.wjg.shokkaa.snmp4jutils.async.snmpAgent
import jp.wjg.shokkaa.snmp4jutils.async.toOid
import jp.wjg.shokkaa.snmp4jutils.scanFlow
import jp.wjg.shokkaa.snmp4jutils.scrambledIpV4AddressSequence
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.VariableBinding
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
    fun scanFlow_response(): Unit = runTest {
        val sampleVB = listOf(VariableBinding("1.3.6.100".toOid(), OctetString("sample data")))
        val localHost = InetAddress.getByName("127.0.0.1")
        val ag = launch { snmpAgent(sampleVB) }
        val req = DefaultSnmpScanRequest(localHost).apply {
            target.timeout = 1000
            target.retries = 1
        }
        val snmp = createDefaultSenderSnmpAsync()
        val ress = flowOf(req).scanFlow(snmp).toList()
        println(ress)
        assert(ress.size == 1)
        val res = ress[0]
        assert(res is Response)
        with(res as Response) {
            println(received.peerAddress.inetAddress)
            println(received.response.variableBindings)
            println(received.response.errorIndex)
            println(received.response.errorStatusText)
            println(received.request.variableBindings)
            assert(received.peerAddress.inetAddress == localHost)
            assert(received.response.variableBindings == sampleVB)
        }
        ag.cancelAndJoin()
    }
}

@file:Suppress("ClassName")

import jp.wjg.shokkaa.snmp4jutils.async.*
import jp.wjg.shokkaa.snmp4jutils.measureThroughput
import jp.wjg.shokkaa.snmp4jutils.scrambledIpV4AddressSequence
import jp.wjg.shokkaa.snmp4jutils.uniCast
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.snmp4j.smi.Integer32
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import java.net.InetAddress
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

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
        val localHost = InetAddress.getByName("127.0.0.1")!!
        val ag = launch { snmpAgent(sampleVB) }
        val req = Request(defaultScanTarget(localHost), defaultPDU())

        val snmp = createDefaultSenderSnmpAsync()
        val ress = flowOf(req).uniCast(snmp).toList()
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

    @Test
    fun scanFlow_timeout(): Unit = runTest {
        val localHost = InetAddress.getByName("127.0.0.1")
        val req = Request(defaultScanTarget(localHost), defaultPDU())
        val snmp = createDefaultSenderSnmpAsync()
        val td = measureTime {
            val ress = flowOf(req).uniCast(snmp).toList()
            assert(ress.size == 1)
            val res = ress[0]
            assert(res is Timeout)
            with(res as Timeout) {
                assert(request.target == req.target)
            }
        }
        assert(td in 2.seconds..2.5.seconds)
    }

    @Test
    fun scanFlow_timeout2(): Unit = runTest {
        val localHost = InetAddress.getByName("127.0.0.1")
        val req = Request(defaultScanTarget(localHost), defaultPDU())
        val snmp = createDefaultSenderSnmpAsync()
        val td = measureTime {
            val ress1 = flowOf(req, req, req).uniCast(snmp, maxSessions = 3).toList()
            assert(ress1.size == 3)
        }
        assert(td in 2.seconds..2.5.seconds)
    }

    @Test
    fun scanFlow_timeout3(): Unit = runTest {
        val localHost = InetAddress.getByName("127.0.0.1")
        val req = Request(defaultScanTarget(localHost), defaultPDU())
        val snmp = createDefaultSenderSnmpAsync()
        val td = measureTime {
            val ress1 = flowOf(req, req, req).uniCast(snmp, maxSessions = 1).toList()
            assert(ress1.size == 3)
        }
        assert(td in 6.seconds..6.5.seconds)
    }

    @Test
    fun scanFlow_wideRange1(): Unit = runTest {
        val localHost = InetAddress.getByName("127.0.0.1")
        val req = Request(defaultScanTarget(localHost), defaultPDU())
        val snmp = createDefaultSenderSnmpAsync()
        measureTime {
            val t1 = flowOf(req).transform { r -> repeat(1000) { emit(r) } }.uniCast(snmp, maxSessions = 1000).count()
            assert(t1 == 1000)
        }.also(::println)
        measureTime {
            val t1 = flowOf(req).transform { r -> repeat(3000) { emit(r) } }.uniCast(snmp, maxSessions = 3000).count()
            assert(t1 == 3000)
        }.also(::println)
    }

    @Test
    fun scanFlow_wideRange_timeout(): Unit = runTest(timeout = 2.hours) {
        val snmp = createDefaultSenderSnmpAsync()
        val localHost = InetAddress.getByName("192.168.20.99")
        val req = Request(defaultScanTarget(localHost), defaultPDU())
        measureTime {
            val t = 16_777_217
            val t1 = flow { repeat(t) { emit(req.apply { pdu.requestID = Integer32(it) }) } }
                .measureThroughput(last = t.toULong()) {
                    uniCast(snmp, maxSessions = 500_000)
                }.count()
            assert(t1 == t)
        }.also(::println)
    }
}
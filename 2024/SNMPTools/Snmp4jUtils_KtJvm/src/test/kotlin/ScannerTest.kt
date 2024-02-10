@file:Suppress("ClassName")

import jp.wjg.shokkaa.snmp4jutils.ULongRangeSet
import jp.wjg.shokkaa.snmp4jutils.async.Received
import jp.wjg.shokkaa.snmp4jutils.async.SnmpTarget
import jp.wjg.shokkaa.snmp4jutils.async.Timeout
import jp.wjg.shokkaa.snmp4jutils.async.createDefaultSenderSnmpAsync
import jp.wjg.shokkaa.snmp4jutils.scanFlow
import jp.wjg.shokkaa.snmp4jutils.scrambledIpV4AddressSequence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock.System.now
import org.junit.jupiter.api.Test
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import java.net.InetAddress
import kotlin.time.Duration.Companion.minutes
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
    fun scanFlow_timeout_Test(): Unit = runTest {
        val s = now()
        fun td() = now() - s
        fun String.toIp4ULong() = InetAddress.getByName(this).address.fold(0UL) { a, e -> (a shl 8) + e.toUByte() }
        val range = "192.168.0.0".toIp4ULong().."192.168.0.0".toIp4ULong()
        with(createDefaultSenderSnmpAsync()) {
            scanFlow(ULongRangeSet(range)) {
                target = SnmpTarget().apply {
                    address = UdpAddress(it, 161)
                    community = OctetString("public")
                    timeout = 1000
                    retries = 2
                }
            }.collect {
                println(td())
                assert(td() in 3.seconds..4.seconds)
            }
        }
        assert(td() in 3.seconds..4.seconds)
    }

    @Test
    fun scanFlow_8bit(): Unit = runTest(timeout = 60.seconds) {
        val s = now()
        fun String.toIp4ULong() = InetAddress.getByName(this).address.fold(0UL) { a, e -> (a shl 8) + e.toUByte() }
        val range = "192.168.0.0".toIp4ULong().."192.168.0.255".toIp4ULong()
        with(createDefaultSenderSnmpAsync()) {
            var totalLength = ULongRangeSet(range).map { it.endInclusive - it.start + 1UL }.sum()
            scanFlow(ULongRangeSet(range)).collect { res ->
                val msg = when (res) {
                    is Timeout -> "${res.request.target.address} Timeout"
                    is Received -> "${res.request.target.address} => ${res.received.peerAddress} ${res.received.response[0].variable}"
                }
                totalLength--
                println("${now() - s} - Res[$totalLength]: $msg")
            }
            assert(totalLength == 0UL)
        }
    }

    @Test
    fun scanFlow_24bit(): Unit = runTest(timeout = 15.minutes) {
        runBlocking(Dispatchers.Default) {
            val s = now()
            fun String.toIp4ULong() = InetAddress.getByName(this).address.fold(0UL) { a, e -> (a shl 8) + e.toUByte() }
            val range = "10.0.0.0".toIp4ULong().."10.255.255.255".toIp4ULong()
            with(createDefaultSenderSnmpAsync()) {
                var totalLength = ULongRangeSet(range).map { it.endInclusive - it.start + 1UL }.sum()
                scanFlow(ULongRangeSet(range), 65536) {
                    target = defaultSnmpFlowTarget(it).apply {
                        timeout = 3000
                        retries = 0
                    }
                }.collect { res ->
                    val msg = when (res) {
                        is Timeout -> "${res.request.target.address} Timeout"
                        is Received -> "${res.request.target.address} =>  ${res.received.response[0].variable}".also(::println)
                    }
                    totalLength--
                    if (totalLength % 65536UL == 0UL) println("${now() - s} - Res[$totalLength]: $msg")
                }
                assert(totalLength == 0UL)
            }
        }
    }
}

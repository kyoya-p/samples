package jp.wjg.shokkaa.snmp.jvm

import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ranges.shouldBeIn
import jp.wjg.shokkaa.snmp.*
import jp.wjg.shokkaa.snmp.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.Path
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import org.snmp4j.smi.UdpAddress
import java.net.InetAddress
import kotlin.time.Clock.System.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.time.measureTime

@OptIn(ExperimentalKoalaPlotApi::class, ExperimentalSerializationApi::class)
class SnmpTest : FunSpec({
    fun around(d: Duration) = (d / 1.1)..(d * 1.1)
    fun Instant.lap() = (now() - this).inWholeMilliseconds.toInt()

    test("RateLimiter0") {
        val ts = now()
        val src = (0..<10).asFlow().onEach { delay(1.milliseconds) }.map { now() - ts }
        measureTime {
            src.buffer().collectIndexed { i, e ->
                println("$e - ${now() - ts}")
            }
        } shouldBeIn 0.microseconds..500.milliseconds
    }
    test("RateLimiter1") {
        val ts = now()
        val src = (0..<10).asFlow().map { now() - ts }
        measureTime {
            src.throttled(RateLimiter(100.milliseconds, 1)).collectIndexed { i, e ->
                println("$e - ${now() - ts}")
                e shouldBeIn 100.milliseconds * i..100.milliseconds * (i + 1)
            }
        } shouldBeIn around(1000.milliseconds)
    }
    test("RateLimiter2") {
        val ts = now()
        val src = (0..<10).asFlow().onEach { delay(1.milliseconds) }.map { now() - ts }
        measureTime {
            src.throttled(RateLimiter(100.milliseconds, 3)).collectIndexed { i, e ->
                println("$e - ${now() - ts}")
                e shouldBeIn 100.milliseconds * (i / 3)..100.milliseconds * (i / 3 + 1)
            }
        } shouldBeIn around(400.milliseconds)
    }

    suspend fun snmpFlowTester(testName: String, tStart: Instant, src: Flow<Request>) {
        val fRes = SystemFileSystem.sink(Path("build/$testName.pb")).buffered()
        val listData = mutableListOf<Log>()
        val snmp = createDefaultSenderSnmp()
        val startAdr = InetAddress.getByName("10.36.0.0").toIpV4UInt()
        src.map { r -> r.copy(userData = (r.userData as Log).copy(t1 = tStart.lap())) }
            .send(snmp).collect { res ->
//            when (res) {
//                is Result.Response -> println("Response ${res.received.response}")
//                is Result.Timeout -> println("Timeout ${res.request}")
//            }
                val d0 = res.request.userData as Log
                val d = d0.copy(t2 = tStart.lap())
                listData.addLast(d)
            }
        fRes.write(ProtoBuf.encodeToByteArray(listData))
        fRes.close()
    }

    test("SnmpSend-0 unlimited") {
        val startAdr = InetAddress.getByName("10.36.0.0").toIpV4UInt()
        val ts = now()
        val src = (0U..<10_000U).asFlow().map { ix ->
            Request(
                udpAdr = UdpAddress((ix + startAdr).toIpV4Adr(), 161),
                nRetry = 0, interval = 1.seconds,
                userData = Log(n = ix.toInt(), ts.lap(), 0, 0),
            )
        }
        snmpFlowTester("SnmpSend-0", ts, src)
    }

    test("SnmpSend-1 1000r/0.5s") {
        val startAdr = InetAddress.getByName("10.36.0.0").toIpV4UInt()
        val rateLimiter = RateLimiter(interval = 0.5.seconds, unit = 1000)
        val ts = now()
        val src = (0U..<10_000U).asFlow().map { ix ->
            Request(
                udpAdr = UdpAddress((ix + startAdr).toIpV4Adr(), 161),
                nRetry = 0, interval = 1.seconds,
                userData = Log(n = ix.toInt(), ts.lap(), 0, 0),
            )
        }.throttled(rateLimiter = rateLimiter)
        snmpFlowTester("SnmpSend-1", ts, src)
    }
    test("SnmpSend-2 1000r/0.1s") {
        val startAdr = InetAddress.getByName("10.36.0.0").toIpV4UInt()
        val rateLimiter = RateLimiter(interval = 0.1.seconds, unit = 1000)
        val ts = now()
        val src = (0U..<10_000U).asFlow().map { ix ->
            Request(
                udpAdr = UdpAddress((ix + startAdr).toIpV4Adr(), 161),
                nRetry = 0, interval = 1.seconds,
                userData = Log(n = ix.toInt(), ts.lap(), 0, 0),
            )
        }.throttled(rateLimiter = rateLimiter)
        snmpFlowTester("SnmpSend-2", ts, src)
    }
    test("SnmpSend-3 3000r/0.1s") {
        val startAdr = InetAddress.getByName("10.36.0.0").toIpV4UInt()
        val rateLimiter = RateLimiter(interval = 0.1.seconds, unit = 3000)
        val ts = now()
        val src = (0U..<10_000U).asFlow().map { ix ->
            Request(
                udpAdr = UdpAddress((ix + startAdr).toIpV4Adr(), 161),
                nRetry = 0, interval = 1.seconds,
                userData = Log(n = ix.toInt(), ts.lap(), 0, 0),
            )
        }.throttled(rateLimiter = rateLimiter)
        snmpFlowTester("SnmpSend-3", ts, src)
    }
    test("SnmpSend-4 5000r/1s") {
        val startAdr = InetAddress.getByName("10.36.0.0").toIpV4UInt()
        val rateLimiter = RateLimiter(interval = 1.seconds, unit = 5000)
        val ts = now()
        val src = (0U..<60_000U).asFlow().map { ix ->
            Request(
                udpAdr = UdpAddress((ix + startAdr).toIpV4Adr(), 161),
                nRetry = 0, interval = 1.seconds,
                userData = Log(n = ix.toInt(), ts.lap(), 0, 0),
            )
        }.throttled(rateLimiter = rateLimiter)
        snmpFlowTester("SnmpSend-4", ts, src)
    }


    test("SnmpSend-5 wide-range") {
        val startAdr = InetAddress.getByName("10.0.0.0").toIpV4UInt()
        val rateLimiter = RateLimiter(interval = 1.seconds, unit = 5000)
        val ts = now()
        val src = (0U..<256U * 256U * 256U).asFlow().map { ix ->
            Request(
                udpAdr = UdpAddress((ix + startAdr).toIpV4Adr(), 161),
                nRetry = 0, interval = 1.seconds,
                userData = Log(n = ix.toInt(), ts.lap(), 0, 0),
            )
        }.throttled(rateLimiter = rateLimiter)
        var c = 0
        val j = launch(Dispatchers.Default) {
            while (true) {
                println("count=$c")
                delay(1.seconds)
            }
        }
        src.send(createDefaultSenderSnmp()).collect { ++c }
        j.cancelAndJoin()
    }
})

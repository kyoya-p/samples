package jp.wjg.shokkaa.snmp.jvm

import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ranges.shouldBeIn
import jp.wjg.shokkaa.snmp.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.io.buffered
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.files.Path
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.protobuf.ProtoBuf
import kotlinx.serialization.protobuf.ProtoNumber
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

    @Serializable
    data class D(
        @ProtoNumber(1) val n: Int,
        @ProtoNumber(2) val t1: Int,
        @ProtoNumber(3) val t2: Int
    )

    suspend fun snmpFlowTester(testName: String, tStart: Instant, src: Flow<Request>) {
        val fRes = SystemFileSystem.sink(Path("build/$testName.pb")).buffered()
        val listData = mutableListOf<D>()
        val snmp = createDefaultSenderSnmp()
        val startAdr = InetAddress.getByName("10.36.0.0").toIpV4UInt()
        src.send(snmp).collect { res ->
//            when (res) {
//                is Result.Response -> println("Response ${res.received.response}")
//                is Result.Timeout -> println("Timeout ${res.request}")
//            }
            val d = D(
                n = (res.request.target.address.inetAddress.toIpV4UInt() - startAdr).toInt(),
                t1 = (res.request.userData as Duration).inWholeMilliseconds.toInt(),
                t2 = (now() - tStart).inWholeMilliseconds.toInt(),
            )
//            println(d)
            listData.addLast(d)
        }
        fRes.write(ProtoBuf.encodeToByteArray(listData))
        fRes.close()
    }

    test("SnmpSend-0") {
        val startAdr = InetAddress.getByName("10.36.0.0").toIpV4UInt()
        val ts = now()
        val src = (0U..<10_000U).asFlow()
            .map { UdpAddress((it + startAdr).toIpV4Adr(), 161) }
            .map { Request(udpAdr = it, nRetry = 0, interval = 1.seconds, userData = now() - ts) }
        snmpFlowTester("SnmpSend-0", ts, src)
    }

    test("SnmpSend-1") {
        val startAdr = InetAddress.getByName("10.36.0.0").toIpV4UInt()
        val rateLimiter = RateLimiter(interval = 1.seconds, unit = 1000)
        val ts = now()
        val src = (0U..<10_000U).asFlow()
            .map { UdpAddress((it + startAdr).toIpV4Adr(), 161) }
            .map { Request(udpAdr = it, nRetry = 0, interval = 1.seconds, userData = now() - ts) }
            .throttled(rateLimiter = rateLimiter)
        snmpFlowTester("SnmpSend-1", ts, src)
    }
})

//@OptIn(ExperimentalKoalaPlotApi::class)
//@Composable
//fun sendRecvGraph(data1: List<Point<Int, Int>>, data2: List<Point<Int, Int>>) {
//    XYGraph(
//        rememberIntLinearAxisModel(data1.autoScaleXRange()),
//        rememberIntLinearAxisModel((data1 + data2).autoScaleYRange()),
//    ) {
//        val dot = @Composable { c: Color -> Symbol(size = 1.dp, fillBrush = SolidColor(c), outlineBrush = null) }
//        LinePlot2(data1, symbol = { dot(Color.Blue) })
//        LinePlot2(data2, symbol = { dot(Color.Red) })
//    }
//}

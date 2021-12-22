import com.sun.jna.Library
import com.sun.jna.Native
import jp.pgw.shokkaa.SnmpAgent
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Semaphore
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime


class Test_Agent {
    fun now() = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        .run { "%4d%02d%02d.%03dZ".format(hour, minute, second, nanosecond/1000/1000) }

    fun test_assertSnmpResponse() {
        val testTarget =
            CommunityTarget(UdpAddress(InetAddress.getByName("localhost"), 161), OctetString("public")).apply {
                timeout = 1000
                retries = 0
            }
        val testReqPdu = PDU(PDU.GET, listOf(VariableBinding(SampleOID.sysDescr.oid)))
        SnmpBuilder().udp().v1().build().use { snmp ->
            val res = snmp.send(testReqPdu, testTarget)
            println("${now()} :res=${res.response}")
            assert(res.response[0].oid == SampleOID.sysDescr.oid)
            assert(res.response[0].variable == OctetString("Sample"))
        }
    }

    @Test
    fun t1_runAgent() {
        val testAgentMibs = mapOf(SampleOID.sysDescr.oid to OctetString("Sample"))

        SnmpAgent(testAgentMibs).use {
            test_assertSnmpResponse()
        }

        // Agent起動を繰り返し
        SnmpAgent(testAgentMibs).use {
            test_assertSnmpResponse()
        }
    }

    @ExperimentalTime
    @ExperimentalCoroutinesApi
    @Test
    fun t2_runAgent_Flow_quickstart(): Unit = runBlocking(Dispatchers.Default) {
        val testAgentMibs = mapOf(SampleOID.sysDescr.oid to OctetString("Sample"))

        val agTask = launch {
            SnmpAgent(testAgentMibs).serviceFlow().collect {} // ずっとblockされるので
        }
        delay(2000.milliseconds)
        agTask.cancelAndJoin() // 2秒後に強制停止

        // 繰り返し
        val agTask2 = launch {
            SnmpAgent(testAgentMibs).serviceFlow().collect {} // ずっとblockされるので
        }
        delay(2000.milliseconds)
        agTask2.cancelAndJoin() // 2秒後に強制停止

    }

    @ExperimentalCoroutinesApi
    @Test
    fun t3_runAgent_Flow(): Unit = runBlocking(Dispatchers.Default) {
        val testAgentMibs = mapOf(SampleOID.sysDescr.oid to OctetString("Sample"))

        val sem = Semaphore(1, 1)
        val agTask = launch {
            SnmpAgent(testAgentMibs).serviceFlow(onStart = { sem.release() }).collect {}
        }
        sem.acquire()
        test_assertSnmpResponse()
        agTask.cancel()
    }

    @Test
    fun iflist() {
        fun Boolean.t(t: String, f: String = "") = if (this) t else f

        for (ni in NetworkInterface.getNetworkInterfaces()) {
            for (a in ni.inetAddresses) {
                val ipv = when (a) {
                    is Inet6Address -> "v6"
                    else -> "v4"
                }

                println("%d: %s%s%s%s %s %s".format(ni.index,
                    ipv,
                    a.isMulticastAddress.t("-MC"),
                    a.isLoopbackAddress.t("-LP"),
                    a.isLinkLocalAddress.t("-LL"),
                    a.hostName,
                    a.address.joinToString(".") { "%d".format(it.toUByte().toInt()) })
                )
            }
            for (a in ni.interfaceAddresses) {
                println("%d-IA %s %s".format(
                    ni.index,
                    a.address,
                    a.broadcast,
                ))
            }
        }
    }

    @Test
    fun jna1() {
        println("${now()}: started")
        Kernel32.INSTANCE.Sleep(1000)
        println("${now()}: finished")
    }
    interface Kernel32 : Library {
        fun Sleep(dwMilliseconds: Int)

        companion object {
            val INSTANCE = Native.load("kernel32", Kernel32::class.java) as Kernel32
        }
    }
}


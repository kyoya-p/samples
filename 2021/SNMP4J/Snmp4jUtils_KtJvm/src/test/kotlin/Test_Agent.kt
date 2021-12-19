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
import java.net.InetAddress
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

class Test_Agent {
    fun now() = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        .run { "%04d%02d%02d.%02d%02d%02dZ".format(year, monthNumber, dayOfMonth, hour, minute, second) }

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
}
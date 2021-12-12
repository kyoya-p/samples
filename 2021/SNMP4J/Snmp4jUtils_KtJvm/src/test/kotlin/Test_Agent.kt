import jp.`live-on`.shokkaa.mibMapTest
import jp.`live-on`.shokkaa.snmpAgentFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
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

class Test_Agent {
    fun now() = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        .run { "%04d%02d%02d.%02d%02d%02dZ".format(year, monthNumber, dayOfMonth, hour, minute, second) }

    val testTarget = CommunityTarget(UdpAddress(InetAddress.getByName("localhost"), 161), OctetString("public")).apply {
        timeout = 1000
        retries = 0
    }
    val testReqPdu = PDU(PDU.GETNEXT, listOf(VariableBinding(SampleOID.sysDescr.oid)))

    @ExperimentalCoroutinesApi
    @Test
    fun t1(): Unit = runBlocking(Dispatchers.Default) {
        println("${now()} t1():1")
        val agTask = launch {
            println("${now()} t1():2")
            snmpAgentFlow(mibMapTest) { ev, resPdu ->
                println("${ev.peerAddress} ${ev.pdu.variableBindings} -> ${resPdu.variableBindings})")
                resPdu
            }.collect { }
        }

        println("${now()} t1():3")
        delay(1000)

        SnmpBuilder().udp().v1().build().use { snmp ->
            println("${now()} t1():5 - send")
            val res = snmp.send(testReqPdu, testTarget)
            println("${now()} :res=${res.response}")
        }
        println("${now()}l t1():6")
        agTask.cancel()
        println("${now()} t1():7")
    }
}
import jp.wjg.shokkaa.SnmpAgent
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import java.net.InetAddress

@Suppress("ClassName")
class Test_Snmp {
    var testAg: SnmpAgent? = null

    @BeforeEach
    fun test_startDummyAgent() {
        println("test_startDummyAgent()")
        val testAgentMibs = mapOf(SampleOID.sysDescr.oid to OctetString("Sample"))
        testAg = SnmpAgent(testAgentMibs).apply { start() }
    }

    @AfterEach
    fun test_stopDummyAgent() {
        println("test_stopDummyAgent()")
        testAg?.close()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun t1_getNext_Timeout() = runBlocking {
        val snmp = SnmpBuilder().udp().v1().threads(1).build().suspendable()

        val pdu = PDU(PDU.GET, listOf(VariableBinding(SampleOID.sysDescr.oid)))
        val tg = CommunityTarget(UdpAddress(InetAddress.getByName("1.2.3.4"), 161), OctetString("public"))
        val r1 = snmp.send(pdu, tg)
        assert(r1.peerAddress == null)
        assert(r1.response?.variableBindings == null)
    }

    @Suppress("RemoveExplicitTypeArguments", "BlockingMethodInNonBlockingContext")
    @Test
    fun t2_getNext() = runBlocking {
        val snmp = SnmpBuilder().udp().v1().threads(1).build().suspendable()

        val pdu = PDU(PDU.GET, listOf(VariableBinding(SampleOID.sysDescr.oid)))
        val tg = CommunityTarget(UdpAddress(InetAddress.getByName("localhost"), 161), OctetString("public"))
        val r0 = snmp.snmp.send(pdu, tg)
        val r1 = snmp.send(pdu, tg)

        assert(r1.peerAddress != null)
        assert(r1.response?.variableBindings != null)
        assert(r0.peerAddress == r1.peerAddress)
        assert(r0.response.variableBindings == r1.response.variableBindings)
        println(r0.peerAddress)
        println(r0.response.variableBindings)
    }

}
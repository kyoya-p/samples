import jp.`live-on`.shokkaa.SampleOID
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import java.net.InetAddress

@Suppress("ClassName")
class Test_Snmp {

    val snmp = SnmpBuilder().udp().v1().threads(1).build().suspendable()

    @Test
    fun test_start_dummyAgent() {
        //SNMPAgent(File(""))
    }

    @Test
    fun test_getNext() = runBlocking {
        val pdu = PDU().apply { variableBindings = listOf(VariableBinding(OID(SampleOID.sysDescr.oid))) }
        val tg = CommunityTarget<UdpAddress>(UdpAddress(InetAddress.getByName("localhost"), 161), OctetString("public"))
        val r0 = snmp.snmp.getNext(pdu, tg)
        val r1 = snmp.getNext(pdu, tg)
        assert(r1.peerAddress != null)
        assert(r1.response?.variableBindings != null)
        assert(r0.peerAddress == r1.peerAddress)
        assert(r0.response.variableBindings == r1.response.variableBindings)
        println(r0.peerAddress)
        println(r0.response.variableBindings)
    }

    @Test
    fun test_getNext_Timeout() = runBlocking {
        val pdu = PDU().apply { variableBindings = listOf(VariableBinding(OID(SampleOID.sysDescr.oid))) }
        val tg = CommunityTarget<UdpAddress>(UdpAddress(InetAddress.getByName("1.2.3.4"), 161), OctetString("public"))
        val r1 = snmp.getNext(pdu, tg)
        assert(r1.peerAddress == null)
        assert(r1.response?.variableBindings == null)
    }
}
import jp.wjg.shokkaa.snmp4jutils.SampleOID
import jp.wjg.shokkaa.snmp4jutils.sendAsync
import jp.wjg.shokkaa.snmp4jutils.suspendable
import kotlinx.coroutines.runBlocking
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

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun t1_getNext_Timeout() = runBlocking {
        val snmp = SnmpBuilder().udp().v1().threads(1).build().suspendable()

        val pdu = PDU(PDU.GET, listOf(VariableBinding(SampleOID.sysDescr.oid)))
        val tg = CommunityTarget(UdpAddress(InetAddress.getByName("1.2.3.4"), 161), OctetString("public"))
        val r1 = snmp.sendAsync(pdu, tg)
        assert(r1 != null)
        assert(r1!!.peerAddress == null)
        assert(r1.response == null)
    }

    @Suppress("RemoveExplicitTypeArguments", "BlockingMethodInNonBlockingContext")
    @Test
    fun t2_getNext() = runBlocking {
        val snmp = SnmpBuilder().udp().v1().threads(1).build().suspendable()

        val pdu = PDU(PDU.GET, listOf(VariableBinding(SampleOID.sysDescr.oid)))
        val tg = CommunityTarget(UdpAddress(InetAddress.getByName("localhost"), 161), OctetString("public"))
        val r0 = snmp.snmp.send(pdu, tg)
        val r1 = snmp.sendAsync(pdu, tg)

        assert(r1?.peerAddress != null)
        assert(r1?.response?.variableBindings != null)
        assert(r0.peerAddress == r1?.peerAddress)
        assert(r0.response.variableBindings == r1?.response?.variableBindings)
        println(r0.peerAddress)
        println(r0.response.variableBindings)
    }

}
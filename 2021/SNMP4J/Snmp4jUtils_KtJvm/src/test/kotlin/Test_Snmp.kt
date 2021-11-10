import jp.`live-on`.shokkaa.SNMPAgent
import jp.`live-on`.shokkaa.TargetOID
import jp.`live-on`.shokkaa.getNext
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
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

    val snmp = SnmpBuilder().udp().v1().threads(1).build()!!

    @Test
    fun test_start_dummyAgent() {
        SNMPAgent()
    }

    @Test
    fun test_getNext() = runBlocking {
        val pdu = PDU().apply { variableBindings = listOf(VariableBinding(OID(TargetOID.sysDescr.oid))) }
        val tg = CommunityTarget<UdpAddress>(UdpAddress(InetAddress.getByName("127.0.0.1"), 161), OctetString("public"))
        snmp.getNext(pdu, tg) {
        }
    }
}
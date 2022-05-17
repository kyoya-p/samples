import jp.wjg.shokkaa.snmp4jutils.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import java.net.InetAddress

internal class CommonKtTest {

    fun CoroutineScope.launchTestAgent() = launch { snmpAgent(sampleMibList) }

    @Test
    fun getAsync(): Unit = runBlocking(Dispatchers.Default) {
        val jobAg = launchTestAgent()

        val snmp = SnmpBuilder().udp().v1().v3().build().suspendable().listen()
        val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID("1.3.6"))))
        val target = CommunityTarget(UdpAddress(InetAddress.getByName("127.0.0.1"), 161), OctetString("public"))
        val res = snmp.sendAsync(pdu, target)
        println(res.response.variableBindings)
        assert(res.response.variableBindings == sampleMibList[0])

        jobAg.cancelAndJoin()
    }
}
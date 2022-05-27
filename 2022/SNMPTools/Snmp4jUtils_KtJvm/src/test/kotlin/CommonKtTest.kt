import jp.wjg.shokkaa.snmp4jutils.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding

internal class CommonKtTest {
    @Test
    fun getAsync(): Unit = runBlocking {
        val snmp = SnmpBuilder().udp().v1().v3().build().suspendable().listen()
//       println("canceled")

        val jobAg = launch { snmpAgent(sampleMibList) }
        delay(1000)

        val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID("1.3.6"))))
        val target = CommunityTarget(UdpAddress(snmp.getInetAddressByName("127.0.0.1"), 161), OctetString("public"))
        println("{1}")
        val res = snmp.sendAsync(pdu, target)
        println("{2}")
        println("${res?.response?.variableBindings}:${sampleMibList[0]}")
        //assert(res.response.variableBindings == sampleMibList[0])
        println("{3}")
        jobAg.cancelAndJoin()
        println("{4}")
    }
}
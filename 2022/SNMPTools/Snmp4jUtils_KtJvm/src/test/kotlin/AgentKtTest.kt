import jp.wjg.shokkaa.snmp4jutils.*
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding

internal class AgentKtTest {
    @Test
    fun snmpReceiverFlow_Termination() = runBlocking {
        val log = mutableListOf("start")
        val jobAgent = launch {
            val snmpAgent = SnmpBuilder().udp().v1().build()
            snmpAgent.listen()
            runCatching {
                log += "launch"
                snmpReceiverFlow(snmpAgent).collectLatest { log += "received" }
            }.onFailure {
                log += "canceled"
            }
            snmpAgent.close()
        }
        val snmpClient = SnmpBuilder().udp().v1().v3().build().suspendable().listen()
        val tg = CommunityTarget(UdpAddress(snmpClient.getInetAddressByName("127.0.0.1"), 161), OctetString("public"))
        delay(2000)
        val r1 = snmpClient.send(PDU(PDU.GETNEXT, listOf(VariableBinding(org.snmp4j.smi.OID("1.3.6")))), tg)
        println(r1?.response)
        delay(1000)
        val r2 = snmpClient.send(PDU(PDU.GETNEXT, listOf(VariableBinding(org.snmp4j.smi.OID("1.3.6")))), tg)
        println(r2?.response)

        delay(5000)
        jobAgent.cancelAndJoin()

        println(log)
        assert(log == mutableListOf("start", "launch", "received", "received", "canceled"))
    }
}
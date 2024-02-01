import jp.wjg.shokkaa.snmp4jutils.async.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import java.net.InetAddress

class CommonKtTest {
    @Test
    fun getAsync(): Unit = runTest {
        val snmp = createDefaultSenderSnmpAsync()

        val jobAg = launch { snmpAgent(sampleMibList) }
        delay(1000)

        val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID("1.3.6"))))
        val target = CommunityTarget(UdpAddress(snmp.getInetAddressByName("127.0.0.1"), 161), OctetString("public"))
        val res = snmp.sendAsync(pdu, target)
        println("${res.response.variableBindings}:${sampleMibList[0]}")
        assert(res.response.variableBindings[0] == sampleMibList[0])
        jobAg.cancelAndJoin()
        snmp.close()
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun getNext_Timeout() = runBlocking {
        val snmp = SnmpBuilder().udp().v1().threads(1).build().async()

        val pdu = PDU(PDU.GET, listOf(VariableBinding(OID(SampleOID.sysDescr.oid))))
        val tg = CommunityTarget(UdpAddress(InetAddress.getByName("1.2.3.4"), 161), OctetString("public"))
        val r1 = snmp.sendAsync(pdu, tg)
        assert(r1.peerAddress == null)
        assert(r1.response == null)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun getNext() = runBlocking(Dispatchers.Default) {
        val jobAg = launch { snmpAgent(sampleMibList) }
        delay(1000)
        val snmpAsync = createDefaultSenderSnmpAsync()
        val pdu = PDU(PDU.GET, listOf(VariableBinding(OID(SampleOID.sysDescr.oid))))
        val tg = CommunityTarget(UdpAddress(InetAddress.getByName("localhost"), 161), OctetString("public"))
        val resAsync = snmpAsync.sendAsync(pdu, tg)
        val resSync = snmpAsync.snmp.send(pdu, tg)

        assert(resAsync.peerAddress != null)
        assert(resAsync.response?.variableBindings != null)
        assert(resSync.peerAddress == resAsync.peerAddress)
        assert(resSync.response.variableBindings == resAsync.response?.variableBindings)

        jobAg.cancelAndJoin()
        snmpAsync.close()
    }

}
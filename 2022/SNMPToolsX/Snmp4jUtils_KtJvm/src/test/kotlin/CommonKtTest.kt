import jp.wjg.shokkaa.snmp4jutils.async.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import java.net.InetAddress

internal class CommonKtTest {
    @Test
    fun getAsync(): Unit = runBlocking {
        val snmp = defaultSenderSnmp

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

    @Suppress("BlockingMethodInNonBlockingContext")
    @Test
    fun getNext_Timeout() = runBlocking {
        val snmp = SnmpBuilder().udp().v1().threads(1).build().async()

        val pdu = PDU(PDU.GET, listOf(VariableBinding(OID(SampleOID.sysDescr.oid))))
        val tg = CommunityTarget(UdpAddress(InetAddress.getByName("1.2.3.4"), 161), OctetString("public"))
        val r1 = snmp.sendAsync(pdu, tg)
        assert(r1 != null)
        assert(r1!!.peerAddress == null)
        assert(r1.response == null)
    }

    @Suppress("RemoveExplicitTypeArguments", "BlockingMethodInNonBlockingContext")
    @Test
    fun getNext() = runBlocking(Dispatchers.Default) {
        val jobAg = launch { snmpAgent(sampleMibList) }
        delay(1000)
        val snmp = defaultSenderSnmp
        val pdu = PDU(PDU.GET, listOf(VariableBinding(OID(SampleOID.sysDescr.oid))))
        val tg = CommunityTarget(UdpAddress(InetAddress.getByName("localhost"), 161), OctetString("public"))
        val r1 = snmp.sendAsync(pdu, tg)
        val r0 = snmp.snmp.send(pdu, tg)

        assert(r1?.peerAddress != null)
        assert(r1?.response?.variableBindings != null)
        assert(r0.peerAddress == r1?.peerAddress)
        assert(r0.response.variableBindings == r1?.response?.variableBindings)

        jobAg.cancelAndJoin()
    }

}
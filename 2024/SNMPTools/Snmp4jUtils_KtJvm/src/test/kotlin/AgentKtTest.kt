import jp.wjg.shokkaa.snmp4jutils.async.*
import jp.wjg.shokkaa.snmp4jutils.decodeFromStream
import jp.wjg.shokkaa.snmp4jutils.yamlSnmp4j
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.smi.*
import java.io.File

internal class AgentKtTest {
    @Test
    fun snmpReceiverFlow_Termination() = runBlocking {
        val log = mutableListOf("start")
        val jobAgent = launch {
            val snmpAgent = SnmpAsync.createDefaultAgentSession().listen()
            runCatching {
                log += "launch"
                snmpReceiverFlow(snmpAgent.snmp).collect { }
                log += "illegal-state"
            }.onFailure {
                log += "canceled"
            }
            snmpAgent.close()
            log += "closed"
        }
        delay(1000)
        jobAgent.cancelAndJoin()

        println(log)
        assert(log == mutableListOf("start", "launch", "canceled", "closed"))
    }

    @Test
    fun snmpReceiverFlow_receive() = runTest {
        val log = mutableListOf<String>()
        val jobAgent = launch {
            SnmpAsync.createDefaultAgentSession().listen().use { snmpAsync ->
                snmpReceiverFlow(snmpAsync.snmp).collect { ev ->
                    log += "received"
                    println(ev.peerAddress)
                    println(ev.pdu)
                }
            }
        }
        delay(1000)
//        yield()
        val snmpClient = createDefaultSenderSnmpAsync()
        val tg = CommunityTarget(
            UdpAddress(snmpClient.getInetAddressByName("127.0.0.1"), 161),
            OctetString("public")
        ).apply {
            timeout = 1000
            retries = 0
        }
        val pdu = PDU(PDU.GETNEXT, listOf(VariableBinding(OID("1.3.6"))))
        pdu.requestID = Integer32(1)
        println("send1")
        val r1 = snmpClient.send(pdu, tg)
        println(r1.response)
        pdu.requestID = Integer32(2)
        val r2 = snmpClient.send(pdu, tg)
        println(r2.response)

        delay(5000)
        jobAgent.cancelAndJoin()

        println(log)
        assert(log == mutableListOf("received", "received"))
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun t_snmpAgent(): Unit = runBlocking(Dispatchers.Default) {
        val file = File("samples/mibWalkTest1.yaml")
        val vbl = yamlSnmp4j.decodeFromStream<List<VariableBinding>>(file.inputStream())

        val ag = launch { snmpAgent(vbl = vbl) }
        println("start walker")
        delay(1000)

        val r = createDefaultSenderSnmpAsync().use { it.walk("localhost").toList().flatten() }
        assert(r.size == vbl.size)
        assert(r.zip(vbl).all { (a, b) -> a == b })
        ag.cancelAndJoin()
    }
}
import jp.wjg.shokkaa.snmp4jutils.decodeFromStream
import jp.wjg.shokkaa.snmp4jutils.snmpAgent
import jp.wjg.shokkaa.snmp4jutils.walk
import jp.wjg.shokkaa.snmp4jutils.yamlSnmp4j
import kotlinx.coroutines.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Test
import org.snmp4j.Snmp
import org.snmp4j.smi.VariableBinding
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.io.File

@ExperimentalSerializationApi
class Test_snmpAgent {
    @Test
    fun t_snmpAgent(): Unit = runBlocking(Dispatchers.Default) {
        @Serializable
        data class D2(
            val ip: String,
            val vbl: List<String>,
        ) //TODO カスタムシリアライザがyamlでdecode時にエラーとなるため、一旦VBLをList<String>してデコード

        val file = File("samples/mibWalktest1.yaml")
        val d2 = yamlSnmp4j.decodeFromStream<D2>(file.inputStream())
        val vbl = d2.vbl.map { yamlSnmp4j.encodeToString(it) }.map { yamlSnmp4j.decodeFromString<VariableBinding>(it) }

        val job = launch {
            println("start snmp agent in coroutine")
            snmpAgent(vbl = vbl)
        }
        println("start walker")
        delay(1000)

        val transport = DefaultUdpTransportMapping()
        val snmp = Snmp(transport)
        transport.listen()

        val r = snmp.walk("localhost").flatMap { it }.toList()
        assert(r.size == vbl.size)
        assert(r.zip(vbl).all { (a, b) -> a == b })
        job.cancelAndJoin()
    }
}


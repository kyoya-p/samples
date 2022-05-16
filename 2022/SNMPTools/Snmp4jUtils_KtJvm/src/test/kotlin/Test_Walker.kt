import jp.wjg.shokkaa.snmp4jutils.*
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.Test
import org.snmp4j.Snmp
import org.snmp4j.smi.VariableBinding
import java.io.File

@Suppress("ClassName")
class Test_Walker {
    @ExperimentalSerializationApi
    @Test
    fun test_walker1() = runBlocking {
        val testMib: List<VariableBinding> =
            yamlSnmp4j.decodeFromStream(File("samples/testMib1.yaml").inputStream())
        val jobAg = launch {
            snmpAgent(testMib) { _, pdu -> pdu.apply(::println) }
        }

        val res = Snmp().apply { listen() }.suspendable().walk("127.0.0.1").map { it.first() }.toList()
        println(res)

        jsonSnmp4j.encodeToStream(res, File("build/testres.json").outputStream())
        //jobAg.cancelAndJoin()
    }
}

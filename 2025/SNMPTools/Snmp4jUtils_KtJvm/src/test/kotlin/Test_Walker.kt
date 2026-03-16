import jp.wjg.shokkaa.snmp4jutils.async.createDefaultSenderSnmpAsync
import jp.wjg.shokkaa.snmp4jutils.async.snmpAgent
import jp.wjg.shokkaa.snmp4jutils.async.walk
import jp.wjg.shokkaa.snmp4jutils.decodeFromStream
import jp.wjg.shokkaa.snmp4jutils.yamlSnmp4j
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.Test
import org.snmp4j.smi.VariableBinding
import java.io.File

@Suppress("ClassName")
class Test_Walker {
    @ExperimentalSerializationApi
    @Test
    fun test_walker1() = runTest {
        val testMib: List<VariableBinding> = yamlSnmp4j.decodeFromStream(File("samples/testMib1.yaml").inputStream())
        val jobAg = launch { snmpAgent(testMib) }

        val res = createDefaultSenderSnmpAsync().walk("127.0.0.1").map { it.first() }.toList()
//        jsonSnmp4j.encodeToStream(res, File("build/testres.json").outputStream())
        assert(res.zip(testMib).any { (r, t) -> r == t })
        jobAg.cancelAndJoin()
    }
}

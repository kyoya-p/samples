import jp.wjg.shokkaa.snmp4jutils.jsonSnmp4j
import jp.wjg.shokkaa.snmp4jutils.yamlSnmp4j
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.junit.jupiter.api.Test
import org.snmp4j.smi.*
import java.net.InetAddress

class SerializerTest {

    val mibs = listOf(
        VariableBinding(OID("1.3.5.0"), OID("1.3.5.1")),
        VariableBinding(OID("1.3.5.1"), Integer32(12345)),
        VariableBinding(OID("1.3.5.2"), OctetString("ABC-123#!$ ")),
        VariableBinding(OID("1.3.5.2.0"), OctetString("")),
        VariableBinding(OID("1.3.5.3"), Gauge32(12345L)),
        VariableBinding(OID("1.3.5.4"), Counter32(12345L)),
        VariableBinding(OID("1.3.5.5"), Counter64(12345L)),
        VariableBinding(OID("1.3.5.6"), Null()),
        VariableBinding(OID("1.3.5.7"), TimeTicks(12345L)),
        VariableBinding(OID("1.3.5.8"), Opaque(ByteArray(5) { it.toByte() })),
        VariableBinding(OID("1.3.5.8.0"), Opaque(ByteArray(0) { it.toByte() })),
        VariableBinding(OID("1.3.5.9"), IpAddress(InetAddress.getByAddress(ByteArray(4) { it.toByte() }))),
    )

    val mibsJson = """[
    "1.3.5.0 6 1.3.5.1",
    "1.3.5.1 2 12345",
    "1.3.5.2 4 ABC-123#!${'$'} ",
    "1.3.5.2.0 4 ",
    "1.3.5.3 66 12345",
    "1.3.5.4 65 12345",
    "1.3.5.5 70 12345",
    "1.3.5.6 5 ",
    "1.3.5.7 67 12345",
    "1.3.5.8 68 :00:01:02:03:04",
    "1.3.5.8.0 68 ",
    "1.3.5.9 64 0.1.2.3"
]"""

    val mibsYaml = """- "1.3.5.0 6 1.3.5.1"
- "1.3.5.1 2 12345"
- "1.3.5.2 4 ABC-123#!${'$'} "
- "1.3.5.2.0 4 "
- "1.3.5.3 66 12345"
- "1.3.5.4 65 12345"
- "1.3.5.5 70 12345"
- "1.3.5.6 5 "
- "1.3.5.7 67 12345"
- "1.3.5.8 68 :00:01:02:03:04"
- "1.3.5.8.0 68 "
- "1.3.5.9 64 0.1.2.3""""

    @Test
    fun serialiser_json() {
        val j = jsonSnmp4j.encodeToString(mibs)
        println(j)
        assert(j == mibsJson)
    }

    @Test
    fun deserialiser_json() {
        val m: List<VariableBinding> = jsonSnmp4j.decodeFromString(mibsJson)
        mibs.zip(m).forEach { (a, b) ->
            println("$a{${a.variable.syntaxString}} : $b{${b.variable.syntaxString}}")
            assert(a == b)
        }
        assert(mibs == m)
    }

    @Test
    fun serialiser_yaml() {
        val y = yamlSnmp4j.encodeToString(mibs)
        println(y)
        assert(y == mibsYaml)
    }

    @Test
    fun deserialiser_yaml() {
        val m: List<VariableBinding> = yamlSnmp4j.decodeFromString(mibsYaml)
        mibs.zip(m).forEach { (a, b) ->
            println("$a{${a.variable.syntaxString}} : $b{${b.variable.syntaxString}}")
            assert(a == b)
        }
        assert(mibs == m)
    }
}
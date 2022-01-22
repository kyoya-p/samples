import com.charleskorn.kaml.Yaml
import jp.wjg.shokkaa.snmp4jutils.*
import kotlinx.coroutines.*
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import org.snmp4j.smi.VariableBinding

@ExperimentalSerializationApi
class Test_snmpAgent {
    @Test
    fun t0(): Unit = runBlocking {

//        val dev = yamlSnmp4j.decodeFromStream<Device>(File("samples/192.168.3.108-2022-01-05.yaml").inputStream())

        // TODO Error
        val t = """
            ip: "123"
            vbl:
              - "1.3.6 4 aaaaa"
              - "1.3.6.4 4 bbbb"
        """.trimIndent()
        val t2 = """
            "1.3.6 4 aaaaa"
        """.trimIndent()
        val t3 = """
            ip: "123"
            vbl: []
        """.trimIndent()

        println(t)
        //val dev = yamlSnmp4j.decodeFromString<Device>(t)
//        snmpAgent(mibMap = dev.vbl.associate { it.oid to it.variable })

        @Serializable
        data class D2(
            val ip: String,
            val vbl: List<String>,
        )
        String.serializer()

        val dev = yamlSnmp4j.decodeFromString<Device>(t)
        println(dev)
    }

}


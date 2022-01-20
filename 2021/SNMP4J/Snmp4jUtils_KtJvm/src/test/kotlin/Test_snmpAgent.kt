import jp.wjg.shokkaa.snmp4jutils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import java.io.File
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime


@ExperimentalSerializationApi
class Test_snmpAgent {
    @Test
    fun t0(): Unit = runBlocking {
//        val dev = yamlSnmp4j.decodeFromStream<Device>(File("samples/192.168.3.108-2022-01-05.yaml").inputStream())


        // TODO Error
        val t = """
            id: 
        """.trimIndent()

        println(t)
        val dev = yamlSnmp4j.decodeFromString<Device>(Device.serializer(),
            t)
        snmpAgent(mibMap = dev.vbl.associate { it.oid to it.variable })
    }

}


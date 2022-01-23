import jp.wjg.shokkaa.snmp4jutils.jsonSnmp4j
import jp.wjg.shokkaa.snmp4jutils.walk
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.Test
import org.snmp4j.CommunityTarget
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import java.io.File
import java.net.InetAddress

@Suppress("ClassName")
class Test_Walker {

    val snmp = SnmpBuilder().udp().v1().threads(1).build()!!
    val target = CommunityTarget<UdpAddress>(UdpAddress(InetAddress.getByName("localhost"), 161), OctetString("public"))

    @ExperimentalSerializationApi
    @Test
    fun test_walker1() {
        val res = snmp.walk(target, listOf(VariableBinding(OID(".1")))).map { it.first() }.toList()
        jsonSnmp4j.encodeToStream(res, File("samples/MX-M6050.json").outputStream())
    }
}

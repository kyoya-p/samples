import jp.wjg.shokkaa.snmp4jutils.jsonSnmp4j
import jp.wjg.shokkaa.snmp4jutils.walk
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.encodeToStream
import org.junit.jupiter.api.Test
import java.io.File

@Suppress("ClassName")
class Test_Walker {
    @ExperimentalSerializationApi
    @Test
    fun test_walker1() {
        val res = walk("localhost").map { it.first() }.toList()
        jsonSnmp4j.encodeToStream(res, File("samples/MX-M6050.json").outputStream())
    }
}

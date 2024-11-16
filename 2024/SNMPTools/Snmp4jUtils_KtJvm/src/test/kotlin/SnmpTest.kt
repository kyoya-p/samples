import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import jp.wjg.shokkaa.snmp4jutils.async.Result
import jp.wjg.shokkaa.snmp4jutils.async.sampleMibList
import jp.wjg.shokkaa.snmp4jutils.async.snmpAgent
import jp.wjg.shokkaa.snmp4jutils.async.snmpAgent_x
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import org.snmp4j.smi.OctetString
import kotlin.collections.LinkedHashMap
import kotlin.time.measureTime

class SnmpTest : FunSpec({
    test(".1.snmpAgent").config(coroutineTestScope = true) {
        val ag = launch { snmpAgent_x(sampleMibList) }
        snmpUnicast("127.0.0.1").getValueOrNull(0) shouldBe OctetString("Dummy SNMP Agent")
        ag.cancelAndJoin()
    }
    test(".2.snmpAgent").config(coroutineTestScope = true) {
        val ag = launch { snmpAgent_x(sampleMibList) }
        snmpUnicast("127.0.0.1").onResponse {
            it.received.response.variableBindings[0].variable.shouldBe(OctetString("Dummy SNMP Agent"))
        }
        ag.cancelAndJoin()
    }
})

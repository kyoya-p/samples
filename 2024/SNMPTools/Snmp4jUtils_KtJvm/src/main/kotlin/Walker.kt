package jp.wjg.shokkaa.snmp4jutils.async

import jp.wjg.shokkaa.snmp4jutils.yamlSnmp4j
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.todayAt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.SMIConstants.EXCEPTION_END_OF_MIB_VIEW
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import java.io.File

@ExperimentalSerializationApi
fun main(args: Array<String>): Unit = runBlocking {
    val st = Clock.System.now()
    SnmpBuilder().udp().v1().build().async().listen().use { snmp ->
        val tgIp = args[0]
        val vbl = snmp.walk(tgIp).map { it[0] }.onEach {
            print("${(Clock.System.now() - st).inWholeMilliseconds}[ms] $it\n")
        }.toList()
        println("${(Clock.System.now() - st).inWholeMilliseconds}[ms] #MIB: ${vbl.size}")

        val resultFile = File("samples/${Clock.System.todayAt(currentSystemDefault())}-walk-$tgIp.yaml")
        resultFile.writeText(yamlSnmp4j.encodeToString(vbl))
    }
}

suspend fun SnmpAsync.walk(
    targetHost: String,
    reqOidList: List<String> = listOf(".1.3.6"),
    port: Int = 161,
    setTarget: CommunityTarget<UdpAddress>.() -> Unit = { timeout = 1000; retries = 3 }
) = walk(
    CommunityTarget(UdpAddress(getInetAddressByName(targetHost), port), OctetString("public")).apply { setTarget() },
    reqOidList.map { VariableBinding(OID(it)) },
)

fun <T> generateFlow(init: T, op: suspend (T) -> T?) = flow {
    var i = init
    while (true) {
        i = op(i) ?: break
        emit(i)
    }
}

suspend fun SnmpAsync.walk(
    target: CommunityTarget<UdpAddress>,
    initVbl: List<VariableBinding>,
): Flow<List<VariableBinding>> = generateFlow(initVbl) { vbl ->
    sendAsync(PDU(PDU.GETNEXT, vbl), target).response
        ?.takeIf { it.errorStatus == PDU.noError }
        ?.variableBindings
        ?.takeIf { it.any { it.variable.syntax != EXCEPTION_END_OF_MIB_VIEW } }
        ?.takeIf { it.zip(initVbl).any { (vb, ivb) -> vb.oid.startsWith(ivb.oid) } }
}




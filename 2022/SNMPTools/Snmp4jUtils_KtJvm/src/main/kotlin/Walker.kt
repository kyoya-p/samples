package jp.wjg.shokkaa.snmp4jutils

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
import org.snmp4j.mp.SnmpConstants.SNMP_ERROR_SUCCESS
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.SMIConstants.EXCEPTION_END_OF_MIB_VIEW
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import java.io.File

@ExperimentalSerializationApi
fun main(args: Array<String>): Unit = runBlocking {
    val st = Clock.System.now()
    SnmpBuilder().udp().v1().build().suspendable().listen().use { snmp ->
        val tgIp = args[0]
        val vbl = snmp.walk(tgIp).map { it[0] }.onEach {
            print("${(Clock.System.now() - st).inWholeMilliseconds}[ms] $it\r")
        }.toList()
        println("${(Clock.System.now() - st).inWholeMilliseconds}[ms] #MIB: ${vbl.size}")

        val resultFile = File("samples/${Clock.System.todayAt(currentSystemDefault())}-walk-$tgIp.yaml")
        resultFile.writeText(yamlSnmp4j.encodeToString(vbl))
    }
}

suspend fun SnmpSuspendable.walk(
    targetHost: String,
    reqOidList: List<String> = listOf(".1.3.6"),
) = walk(
    CommunityTarget(UdpAddress(getInetAddressByName(targetHost), 161), OctetString("public"))
        .apply {
            timeout = 1000
            retries = 3
        },
    reqOidList.map { VariableBinding(OID(it)) },
)

fun <T> generateFlow(init: T, op: suspend (T) -> T?) = flow {
    var i = init
    while (true) {
        i = op(i) ?: break
        emit(i)
    }
}

suspend fun SnmpSuspendable.walk(
    target: CommunityTarget<UdpAddress>,
    initVbl: List<VariableBinding>,
): Flow<List<VariableBinding>> = generateFlow(initVbl) { vbl ->
    sendAsync(PDU(PDU.GETNEXT, vbl), target)
        ?.response?.takeIf { it.errorStatus == SNMP_ERROR_SUCCESS }
        ?.variableBindings?.takeIf {
            it.zip(initVbl)
                .any { (vb, ivb) -> vb.variable.syntax != EXCEPTION_END_OF_MIB_VIEW && vb.oid.startsWith(ivb.oid) }
        }
}




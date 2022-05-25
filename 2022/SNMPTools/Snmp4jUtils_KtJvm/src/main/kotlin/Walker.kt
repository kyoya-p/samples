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
import org.snmp4j.Snmp
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.SMIConstants.*
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.Variable
import org.snmp4j.smi.VariableBinding
import java.io.File
import java.net.InetAddress

@ExperimentalSerializationApi
fun main(args: Array<String>): Unit = runCatching {
    val snmp = SnmpBuilder().udp().v1().build().suspendable()
    runBlocking {
        val tgIp = args[0]
        val resultFile = File("samples/${Clock.System.todayAt(currentSystemDefault())}-walk-$tgIp.yaml")

        val vbl = snmp.walk(tgIp).map { it[0] }.onEach { println(it) }.toList()
        val dev = Device(tgIp, vbl)
        resultFile.writeText(yamlSnmp4j.encodeToString(dev))
    }
}.onFailure {
    println("usage java -jar ip-address")
}.getOrThrow()

@Suppress("unused")
fun mainSplit(args: Array<String>): Unit = runCatching {
    runBlocking {
        val tgIp = args[0]

        var rep = ""
        var repIndex = 0
        Snmp().suspendable().walk(tgIp).collect { vbl -> //ペイロードサイズが256B以上になればレポート送信
            rep = rep + vbl.joinToString("\n", "", "\n")
            if (rep.length >= 256) {
                sendReport(repIndex, rep, false)
                rep = ""
                repIndex++
            }
        }
        sendReport(repIndex, rep, true) //空でもcomplete通知のためにレポートを送信する場合がある
    }
}.onFailure {
    println("usage java WalkerKt")

}.getOrThrow()

suspend fun SnmpSuspendable.walk(
    targetHost: String,
    reqOidList: List<String> = listOf(".1.3.6"),
): Flow<List<VariableBinding>> = walk(
    CommunityTarget(UdpAddress(InetAddress.getByName(targetHost), 161), OctetString("public"))
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
): Flow<List<VariableBinding>> {
    return generateFlow(initVbl) { vbl ->
        fun Variable.isNotNormal() = when (syntax) {
            EXCEPTION_NO_SUCH_OBJECT, EXCEPTION_NO_SUCH_INSTANCE, EXCEPTION_END_OF_MIB_VIEW -> true
            else -> false
        }
        sendAsync(PDU(PDU.GETNEXT, vbl), target).response?.variableBindings?.takeIf {
            // 全要素 EndOfViewか初期OIDを超えたら終了
            it.zip(initVbl).any { (vb, ivb) -> vb.variable.isNotNormal() && vb.oid.startsWith(ivb.oid) }
        }
    }
}

private fun sendReport(index: Int, rep: String, complete: Boolean) {
    println("Report: {")
    println("  index: $index,")
    println("  complete: $complete,")
    print(rep)
    println("}")
}
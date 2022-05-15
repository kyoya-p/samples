package jp.wjg.shokkaa.snmp4jutils


import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone.Companion.currentSystemDefault
import kotlinx.datetime.todayAt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.smi.OID
import org.snmp4j.smi.OctetString
import org.snmp4j.smi.UdpAddress
import org.snmp4j.smi.VariableBinding
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.io.File
import java.net.InetAddress

@ExperimentalSerializationApi
fun main(args: Array<String>): Unit = runCatching {
    val tgIp = args[0]
    val resultFile = File("samples/${Clock.System.todayAt(currentSystemDefault())}-walk-$tgIp.yaml")

    val vbl = walk(tgIp).flatMap { it }.onEach { println(it) }.toList()
    val dev = Device(tgIp, vbl)
    resultFile.writeText(yamlSnmp4j.encodeToString(dev))
}.onFailure {
    println("usage java -jar ip-address")
}.getOrThrow()

@Suppress("unused")
fun mainSplit(args: Array<String>): Unit = runCatching {
    val tgIp = args[0]

    var rep = ""
    var repIndex = 0
    walk(tgIp).forEach { vbl -> //ペイロードサイズが256B以上になればレポート送信
        rep = rep + vbl.joinToString("\n", "", "\n")
        if (rep.length >= 256) {
            sendReport(repIndex, rep, false)
            rep = ""
            repIndex++
        }
    }
    sendReport(repIndex, rep, true) //空でもcomplete通知のためにレポートを送信する場合がある

}.onFailure {
    println("usage java WalkerKt")

}.getOrThrow()

suspend fun walk(
    targetHost: String,
    reqOidList: List<String> = listOf(".1.3.6"),
    snmp: Snmp = Snmp(DefaultUdpTransportMapping().apply { listen() }),
): Sequence<List<VariableBinding>> = walk(
    CommunityTarget(UdpAddress(InetAddress.getByName(targetHost), 161), OctetString("public"))
        .apply {
            timeout = 1000
            retries = 3
        },
    reqOidList.map { VariableBinding(OID(it)) },
    snmp,
)

suspend fun walk(
    target: CommunityTarget<UdpAddress>,
    initVbl: List<VariableBinding>,
    snmp: Snmp = Snmp(DefaultUdpTransportMapping().apply { listen() }),
): Sequence<List<VariableBinding>> {
    return generateSequence(initVbl) { vbl ->
        println(target)
        snmp.suspendable()
            .sendAsync(PDU(PDU.GETNEXT, vbl), target)?.response?.takeIf { it.errorStatus == PDU.noError }?.variableBindings
    }.drop(1).takeWhile { vb -> // 全要素 EndOfViewか初期OIDを超えたら終了
        vb.zip(initVbl).any { (vb, ivb) -> vb.variable.syntax != 130 && vb.oid.startsWith(ivb.oid) }
    }
}

private fun sendReport(index: Int, rep: String, complete: Boolean) {
    println("Report: {")
    println("  index: $index,")
    println("  complete: $complete,")
    print(rep)
    println("}")
}
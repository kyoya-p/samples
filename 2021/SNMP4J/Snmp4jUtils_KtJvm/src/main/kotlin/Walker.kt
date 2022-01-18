package jp.wjg.shokkaa.snmp4jutils


import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayAt
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.mp.SnmpConstants
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
    val resultFile = File("samples/$tgIp-${Clock.System.todayAt(TimeZone.currentSystemDefault())}.yaml")

    val transport = DefaultUdpTransportMapping()
    val snmp = Snmp(transport)
    transport.listen()

    val targetAddress = UdpAddress(InetAddress.getByName(tgIp), 161)
    val target = CommunityTarget<UdpAddress>(targetAddress, OctetString("public"))
    target.version = SnmpConstants.version2c

    val initVbl = listOf(VariableBinding(OID(".1"))/*(お試し)複数OID取得*/)
    val vbl = snmp.walk(initVbl, target).flatMap { it }.toList()
    val dev = Device(tgIp, vbl)
    resultFile.appendText(yamlSnmp4j.encodeToString(dev))
}.onFailure {
    println("usage java -jar ")
}.getOrThrow()

@Suppress("unused")
fun mainSplit(args: Array<String>): Unit = runCatching {
    val tgIp = args[0]

    val transport = DefaultUdpTransportMapping()
    val snmp = Snmp(transport)
    transport.listen()

    val targetAddress = UdpAddress(InetAddress.getByName(tgIp), 161)
    val target = CommunityTarget<UdpAddress>(targetAddress, OctetString("public"))
    target.version = SnmpConstants.version2c

    var rep = ""
    var repIndex = 0

    val initVbl = listOf(VariableBinding(OID(".1"))/*(お試し)複数OID取得*/)
    snmp.walk(initVbl, target).forEach { vbl -> //ペイロードサイズが256B以上になればレポート送信
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

fun Snmp.walk(initVbl: List<VariableBinding>, target: CommunityTarget<UdpAddress>) =
    generateSequence(initVbl) { vbl -> // SNMP-Walk
        send(PDU(PDU.GETNEXT, vbl), target)?.response?.takeIf { it.errorStatus == PDU.noError }?.variableBindings
    }.drop(1).takeWhile { vb -> // 全要素 EndOfViewか初期OIDを超えたら終了
        vb.zip(initVbl).any { (vb, ivb) -> vb.variable.syntax != 130 && vb.oid.startsWith(ivb.oid) }
    }


fun sendReport(index: Int, rep: String, complete: Boolean) {
    println("Report: {")
    println("  index: $index,")
    println("  complete: $complete,")
    print(rep)
    println("}")
}
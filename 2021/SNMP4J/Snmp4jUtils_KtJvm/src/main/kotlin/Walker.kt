package jp.`live-on`.shokkaa

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.net.InetAddress

fun main() {
    val transport = DefaultUdpTransportMapping()
    val snmp = Snmp(transport)
    transport.listen()

    val targetAddress = UdpAddress(InetAddress.getByName("localhost"), 161)
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
    sendReport(repIndex, rep, true) //空でもcmplのためにレポートを送信する場合がある
}

fun Snmp.walk(initVbl: List<VariableBinding>, target: CommunityTarget<UdpAddress>) =
    generateSequence(initVbl) { vbl -> // SNMP-Walk
        send(PDU(PDU.GETNEXT, vbl), target)?.response?.takeIf { it.errorStatus == PDU.noError }?.variableBindings
    }.drop(1).takeWhile { vb -> // 全要素 EndOfViewか初期OIDを超えたら終了
        vb.zip(initVbl).any { (vb, ivb) -> vb.variable.syntax != 130 && vb.oid.startsWith(ivb.oid) }
    }

fun sendReport(index: Int, rep: String, cmpl: Boolean) {
    println("Report: {")
    println("  index: $index,")
    println("  complete: $cmpl,")
    print(rep)
    println("}")
}
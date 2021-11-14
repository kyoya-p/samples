package jp.`live-on`.shokkaa

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.mp.SnmpConstants
import org.snmp4j.smi.*
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.io.File
import java.net.InetAddress

@Serializable
data class VBYaml(val mib: String, val value: String)

@Serializable
data class DevYaml(val ip: String, val vbs: List<VBYaml>)

fun main(args: Array<String>) {
    val devIps =
        Yaml.default.decodeFromString<List<DevYaml>>(File(args[0]).readText())
            .map { dev -> dev.ip }
            .distinct()
    println(devIps)
    println(devIps.size)
    return

    val transport = DefaultUdpTransportMapping()
    val snmp = Snmp(transport)
    transport.listen()

    devIps.forEach { ip ->
        val targetAddress = UdpAddress(InetAddress.getByName(ip), 161)
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
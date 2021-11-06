import kotlinx.coroutines.*
import org.snmp4j.PDU
import org.snmp4j.Snmp
import org.snmp4j.event.ResponseEvent
import org.snmp4j.event.ResponseListener
import org.snmp4j.fluent.SnmpBuilder
import org.snmp4j.smi.*
import org.snmp4j.Target
import java.math.BigInteger
import java.net.InetAddress
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


@FlowPreview
suspend fun main(args: Array<String>) = runBlocking {
    val netAdr = BigInteger(InetAddress.getByName(args.getOrNull(0) ?: "192.168.3.0").address).toLong()
    val scanMask = args.getOrNull(1)?.toInt() ?: 4

    val snmpBuilder = SnmpBuilder()
    val snmp = snmpBuilder.udp().v1().threads(1).build()
    snmp.listen()

    var c = 0
    (0L until (1 shl scanMask)).forEach { i ->
        launch {
            // val adrBytes = (netAdr or i).toBigInteger().toByteArray()
            val adrBytes = (netAdr or i.reverseBit32(scanMask)).toBigInteger().toByteArray()
            val udpAdr = UdpAddress(InetAddress.getByAddress(adrBytes), 161)
            print("\r%d/%s/%d/%d: ".format(i, udpAdr.inetAddress.hostAddress, 1 shl scanMask, ++c))
            val targetBuilder = snmpBuilder.target(udpAdr)
            val target = targetBuilder.community(OctetString("public"))
                .timeout(1000).retries(5)
                .build()
            val pdu = PDU().apply {
                type = PDU.GETNEXT
                requestID = Integer32(i.toInt())
                variableBindings = TargetOID.values().map { VariableBinding(OID(it.oid)) }
            }

            val r = snmp.sendCor(pdu, target)
            println(r)
            r?.let { ev ->
                println(
                    "${udpAdr.inetAddress.hostAddress} ${ev.peerAddress}  ${
                        ev.response?.variableBindings?.getOrNull(0)
                    }"
                )
            }
            c--
        }
        delay(10)
    }
    snmp.close()
}

// callbackをcoroutineに変換
suspend fun Snmp.sendCor(pdu: PDU, target: Target<UdpAddress>) =
    suspendCoroutine<ResponseEvent<UdpAddress>?> { continuation ->
        send(pdu, target, null, object : ResponseListener {
            override fun <A : Address?> onResponse(ev: ResponseEvent<A>?) {
                continuation.resume(when {
                    ev == null || ev.response == null -> null
                    ev.peerAddress == target.address -> ev as ResponseEvent<UdpAddress>
                    else -> null
                })
                //this@sendCor.cancel(pdu, this)
            }
        })
    }

fun Long.reverseBit32(width: Int = 32): Long {
    var x = this
    x = ((x and 0x55555555) shl 1) or ((x and 0xAAAAAAAA) ushr 1)
    x = ((x and 0x33333333) shl 2) or ((x and 0xCCCCCCCC) ushr 2)
    x = ((x and 0x0F0F0F0F) shl 4) or ((x and 0xF0F0F0F0) ushr 4)
    x = ((x and 0x00FF00FF) shl 8) or ((x and 0xFF00FF00) ushr 8)
    return ((x shl 16) or (x ushr 16)) ushr (32 - width)
}

enum class TargetOID(val oid: String, val oidName: String) {
    sysDescr("1.3.6.1.2.1.1.1", "sysDescr"),
    sysName("1.3.6.1.2.1.1.5", "sysName"),
    hrDeviceDescr("1.3.6.1.2.1.25.3.2.1.3", "hrDeviceDescr"),
    prtGeneralPrinterName("1.3.6.1.2.1.43.5.1.1.16", "prtGeneralPrinterName"),
    prtInputVendorName("1.3.6.1.2.1.43.8.2.1.14", "prtInputVendorName"),
    prtOutputVendorName("1.3.6.1.2.1.43.9.2.1.8", "prtOutputVendorName"),
}

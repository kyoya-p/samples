package jp.wjg.shokkaa.snmp.jvm

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.mpp.start
import jp.wjg.shokkaa.snmp.Request
import jp.wjg.shokkaa.snmp.Result
import jp.wjg.shokkaa.snmp.createDefaultSenderSnmp
import jp.wjg.shokkaa.snmp.send
import jp.wjg.shokkaa.snmp.toIpV4Adr
import jp.wjg.shokkaa.snmp.toIpV4UInt
import jp.wjg.shokkaa.snmp.toIpV4ULong
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import org.snmp4j.CommandResponder
import org.snmp4j.CommandResponderEvent
import org.snmp4j.security.SecurityModels
import org.snmp4j.security.SecurityProtocols
import org.snmp4j.security.USM
import org.snmp4j.smi.Address
import org.snmp4j.smi.UdpAddress
import org.snmp4j.transport.DefaultUdpTransportMapping
import org.snmp4j.transport.UdpTransportMapping
import java.net.Inet4Address
import java.net.InetAddress
import kotlin.time.Clock.System.now
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

class SnmpTest : FunSpec({
    test("SnmpSend") {
        val file = SystemFileSystem.sink(Path("test.csv")).buffered()

        val snmp = createDefaultSenderSnmp()
        val startAdr = InetAddress.getByName("10.0.0.0").toIpV4UInt()
        val ts = now()
        (0U..<10000U).asFlow().map { UdpAddress((it + startAdr).toIpV4Adr(), 161) }
            .map { Request(udpAdr = it, userData = now() - ts) }.send(snmp).collect { res ->
                when (res) {
                    is Result.Response -> println("Response ${res.received.response}")
                    is Result.Timeout -> {
                        val r =
                            "${res.request.target.address.inetAddress.toIpV4UInt() - startAdr},${(res.request.userData as Duration).inWholeMilliseconds},${(now() - ts).inWholeMilliseconds}\n"
                        print(r)
                        file.writeString(r)
                    }
                }
            }
        delay(1000.milliseconds)
    }
})


//suspend fun Snmp.snmpReceiver(
//    udpAdr: UdpAddress,
//    transport: UdpTransportMapping = DefaultUdpTransportMapping(udpAdr.toRaw()),
//    onResult: Snmp.(SnmpReceived) -> Unit
//) {
////        val transport = DefaultUdpTransportMapping(udpAdr.toRaw())
//    val usm = USM(
//        SecurityProtocols.getInstance().addDefaultProtocols(),
//        org.snmp4j.smi.OctetString(MessageProcessingModel.MPv3.createLocalEngineID()),
//        0
//    )
//    SecurityModels.getInstance().addSecurityModel(usm)
//    val snmp = (this as SnmpImpl).snmp
//    snmp.messageDispatcher.addMessageProcessingModel(MessageProcessingModel.MPv1())
//    snmp.messageDispatcher.addMessageProcessingModel(MessageProcessingModel.MPv2c())
//    snmp.messageDispatcher.addMessageProcessingModel(MessageProcessingModel.MPv3(usm))
//
//    snmp.addCommandResponder(object : CommandResponder {
//        override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
//            @Suppress("UNCHECKED_CAST")
//            if (event != null && event.peerAddress is org.snmp4j.smi.UdpAddress) {
//                val res = SnmpReceived(
//                    peerAddress = event.peerAddress as UdpAddress,
//                    mpModelCode = event.messageProcessingModel,
//                    secModel = event.securityModel,
//                    secName = event.securityName,
//                    secLevel = event.securityLevel,
//                    pdu = event.pdu.toPDU(),
//                    maxSizeResponseScopedPDU = event.maxSizeResponsePDU,
//                    stateReference_todo = null //  event.stateReference,
//                )
//                onResult(res)
//            }
//        }
//    })
//
//    snmp.listen()
//    try {
//        awaitCancellation()
//    } finally {
//        snmp.close()
//    }
//}



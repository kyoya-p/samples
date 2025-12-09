package jp.wjg.shokkaa.snmp.jvm

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import jp.wjg.shokkaa.snmp.Exception
import jp.wjg.shokkaa.snmp.Inet4Address
import jp.wjg.shokkaa.snmp.OctetString
import jp.wjg.shokkaa.snmp.PDU
import jp.wjg.shokkaa.snmp.Request
import jp.wjg.shokkaa.snmp.Response
import jp.wjg.shokkaa.snmp.Snmp
import jp.wjg.shokkaa.snmp.SnmpReceived
import jp.wjg.shokkaa.snmp.Target
import jp.wjg.shokkaa.snmp.Timeout
import jp.wjg.shokkaa.snmp.UdpAddress
import jp.wjg.shokkaa.snmp.createSnmp
import jp.wjg.shokkaa.snmp.toIpv4
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.snmp4j.CommandResponder
import org.snmp4j.CommandResponderEvent
import org.snmp4j.security.SecurityModels
import org.snmp4j.security.SecurityProtocols
import org.snmp4j.security.USM
import org.snmp4j.smi.Address
import org.snmp4j.transport.DefaultUdpTransportMapping
import org.snmp4j.transport.UdpTransportMapping
import kotlin.time.Duration.Companion.milliseconds

class SnmpTest : FunSpec({
    test("SnmpAddress") {
        fun String.toByteArray() = split(".").map { it.toUByte().toByte() }.toByteArray()
        val a1 = Inet4Address("1.2.3.4".toByteArray())
        val a2 = a1.toRaw()
        a2 shouldBe java.net.InetAddress.getByAddress("1.2.3.4".toByteArray())

        val u1 = UdpAddress(Inet4Address("255.255.255.255".toByteArray()), 256)
        val u2: org.snmp4j.smi.UdpAddress = u1.toRaw()
        u2.inetAddress.address shouldBe "255.255.255.255".toByteArray()
        u2.port shouldBe 256

        val u3: UdpAddress = u2.toUdpAddress()
        u3.address.address shouldBe u1.address.address
        u3.port shouldBe u1.port
    }

    test("SnmpSend") {
        launch {
            val snmp = createSnmp(UdpAddress(Inet4Address(byteArrayOf(127, 0, 0, 1))), 1024)
            snmp.snmpReceiver(UdpAddress(Inet4Address(byteArrayOf(127, 0, 0, 1)), 10161)) {
                println(it)
            }
        }
        val snmp = createSnmp(UdpAddress(Inet4Address(byteArrayOf(127, 0, 0, 1))),1024)
        val adr = UdpAddress("127.0.0.1".toIpv4(), 10161)
        snmp.send(
            Request(Target(adr, OctetString("public".toByteArray())), pdu = PDU())
        ) {
            println("--------------------------")
            when (it) {
                is Response -> print("Response ${it.pdu}")
                is Timeout -> println("Timeout")
                is Exception -> println("Exception: ${it.exception.stackTraceToString()}")
            }
        }
        delay(1000.milliseconds)
    }

    //    test("Snmp Agent") {
//        val port = 16100
//        val community = "public"
//        val oid = ".1.3.6.1.2.1.1.1.0"
//        val responseText = "Hello SNMP"
//
//        // Start Agent
//        val agJob = launch(Dispatchers.IO) {
//            snmpReceiver("127.0.0.1/$port") { recv ->
//                val pdu = recv.pdu
//                if (pdu != null) {
//                    val responsePdu = PDU(pdu)
//                    responsePdu.type = PDU.RESPONSE
//                    responsePdu.errorStatus = PDU.noError
//                    responsePdu.errorIndex = 0
//                    responsePdu.variableBindings.firstOrNull()?.let { vb ->
//                        vb.variable = OctetString(responseText)
//                    }
//                    messageDispatcher.returnResponsePdu<UdpAddress>(
//                        recv.mpModelCode,
//                        recv.secModel,
//                        recv.secName,
//                        recv.secLevel,
//                        responsePdu,
//                        recv.maxSizeResponseScopedPDU,
//                        recv.stateReference,
//                        StatusInformation()
//                    )
//                }
//            }
//        }
//
//        delay(100) // Wait for agent to start
//        val snmp = Snmp(DefaultUdpTransportMapping())
//        val targetAddress = UdpAddress("127.0.0.1/$port")
//        val pdu = PDU()
//        pdu.add(VariableBinding(OID(oid)))
//        pdu.type = PDU.GET
//        val result = snmp.get(pdu, CommunityTarget(targetAddress, OctetString(community)))
//
//        // Send Request
//
//        // Verify Response
//        val response = result.response.variableBindings
//        response shouldNotBe null
////        response?.response?.variableBindings?.firstOrNull()?.variable?.toString() shouldBe responseText
////
//        agJob.cancel()
//    }
//})
//

})


suspend fun Snmp.snmpReceiver(
    udpAdr: UdpAddress,
    transport: UdpTransportMapping = DefaultUdpTransportMapping(udpAdr.toRaw()),
    onResult: Snmp.(SnmpReceived) -> Unit
) {
//        val transport = DefaultUdpTransportMapping(udpAdr.toRaw())
    val usm = USM(
        SecurityProtocols.getInstance().addDefaultProtocols(),
        org.snmp4j.smi.OctetString(MessageProcessingModel.MPv3.createLocalEngineID()),
        0
    )
    SecurityModels.getInstance().addSecurityModel(usm)
    val snmp = (this as SnmpImpl).snmp
    snmp.messageDispatcher.addMessageProcessingModel(MessageProcessingModel.MPv1())
    snmp.messageDispatcher.addMessageProcessingModel(MessageProcessingModel.MPv2c())
    snmp.messageDispatcher.addMessageProcessingModel(MessageProcessingModel.MPv3(usm))

    snmp.addCommandResponder(object : CommandResponder {
        override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
            @Suppress("UNCHECKED_CAST")
            if (event != null && event.peerAddress is org.snmp4j.smi.UdpAddress) {
                val res = SnmpReceived(
                    peerAddress = event.peerAddress as UdpAddress,
                    mpModelCode = event.messageProcessingModel,
                    secModel = event.securityModel,
                    secName = event.securityName,
                    secLevel = event.securityLevel,
                    pdu = event.pdu.toPDU(),
                    maxSizeResponseScopedPDU = event.maxSizeResponsePDU,
                    stateReference_todo = null //  event.stateReference,
                )
                onResult(res)
            }
        }
    })

    snmp.listen()
    try {
        awaitCancellation()
    } finally {
        snmp.close()
    }
}



import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import jp.wjg.shokkaa.snmp.toIpV4String
import jp.wjg.shokkaa.snmp.toIpV4UInt
import kotlinx.coroutines.delay
import org.snmp4j.CommandResponder
import org.snmp4j.CommandResponderEvent
import org.snmp4j.Snmp
import org.snmp4j.security.SecurityProtocols
import org.snmp4j.security.USM
import org.snmp4j.smi.OctetString
import org.snmp4j.transport.DefaultUdpTransportMapping
import io.kotest.matchers.shouldNotBe
import jp.wjg.shokkaa.snmp.MessageProcessingModel
import jp.wjg.shokkaa.snmp.Result
import jp.wjg.shokkaa.snmp.SnmpReceived
import jp.wjg.shokkaa.snmp.UdpAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import org.snmp4j.CommunityTarget
import org.snmp4j.PDU
import org.snmp4j.mp.MPv3
import org.snmp4j.mp.StatusInformation
import org.snmp4j.security.SecurityModels
import org.snmp4j.smi.Address
import org.snmp4j.smi.OID
import org.snmp4j.smi.VariableBinding

class SnmpTest : FunSpec({
    test("Snmp IP") {
        "1.2.3.4".toIpV4UInt() shouldBe 0x01_02_03_04u
        "255.255.255.255".toIpV4UInt() shouldBe 0xff_ff_ff_ffu
        "0.0.0.1000".toIpV4UInt() shouldBe 1000u // Not the usual way

        0x04_03_02_01u.toIpV4String() shouldBe "4.3.2.1"
        0xff_ff_ff_ffu.toIpV4String() shouldBe "255.255.255.255"
    }

    test("Snmp Agent") {
        val port = 16100
        val community = "public"
        val oid = ".1.3.6.1.2.1.1.1.0"
        val responseText = "Hello SNMP"

        // Start Agent
        val agJob = launch(Dispatchers.IO) {
            snmpReceiver("127.0.0.1/$port") { recv ->
                val pdu = recv.pdu
                if (pdu != null) {
                    val responsePdu = PDU(pdu)
                    responsePdu.type = PDU.RESPONSE
                    responsePdu.errorStatus = PDU.noError
                    responsePdu.errorIndex = 0
                    responsePdu.variableBindings.firstOrNull()?.let { vb ->
                        vb.variable = OctetString(responseText)
                    }
                    messageDispatcher.returnResponsePdu<UdpAddress>(
                        recv.mpModelCode,
                        recv.secModel,
                        recv.secName,
                        recv.secLevel,
                        responsePdu,
                        recv.maxSizeResponseScopedPDU,
                        recv.stateReference,
                        StatusInformation()
                    )
                }
            }
        }

        delay(100) // Wait for agent to start
        val snmp = Snmp(DefaultUdpTransportMapping())
        val targetAddress = UdpAddress("127.0.0.1/$port")
        val pdu = PDU()
        pdu.add(VariableBinding(OID(oid)))
        pdu.type = PDU.GET
        val result = snmp.get(pdu, CommunityTarget(targetAddress, OctetString(community)))

        // Send Request

        // Verify Response
        val response = result.response.variableBindings
        response shouldNotBe null
        response?.response?.variableBindings?.firstOrNull()?.variable?.toString() shouldBe responseText

        job.cancel()
    }
}
)

suspend fun snmpReceiver(udpAdr: String, onResult: Snmp.(SnmpReceived) -> Unit) {
    val address = UdpAddress(udpAdr)
    val transport = DefaultUdpTransportMapping(address)
    val snmp = Snmp(transport)

    val usm = USM(
        SecurityProtocols.getInstance().addDefaultProtocols(),
        OctetString(MPv3.createLocalEngineID()), 0
    )
    SecurityModels.getInstance().addSecurityModel(usm)

    snmp.messageDispatcher.addMessageProcessingModel(MessageProcessingModel.MPv1())
    snmp.messageDispatcher.addMessageProcessingModel(MPv2c())
    snmp.messageDispatcher.addMessageProcessingModel(MPv3(usm))

    snmp.addCommandResponder(object : CommandResponder {
        override fun <A : Address?> processPdu(event: CommandResponderEvent<A>?) {
            @Suppress("UNCHECKED_CAST")
            if (event != null && event.peerAddress is UdpAddress) {
                val res = SnmpReceived(
                    peerAddress = event.peerAddress as UdpAddress,
                    mpModel = TODO(),
                    secModel = TODO(),
                    secName = TODO(),
                    secLevel = TODO(),
                    pdu = TODO(),
                    maxSizeResponseScopedPDU = TODO(),
                    stateReference = TODO()
                )
                snmp.onResult(res)
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
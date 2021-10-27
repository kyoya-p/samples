package mibtool

import gdvm.device.PDU
import gdvm.device.SnmpTarget
import kotlinx.serialization.*
import java.net.InetAddress

@Serializable
data class ResponseEvent(
        val reqTarget: SnmpTarget,
        val reqPdu: PDU,
        val resTarget: SnmpTarget,
        val resPdu: PDU,
)


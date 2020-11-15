package mibtool

import gdvm.agent.mib.PDU
import gdvm.agent.mib.SnmpTarget
import kotlinx.serialization.*
import java.net.InetAddress

@Serializable
data class ResponseEvent(
        val reqTarget: SnmpTarget,
        val reqPdu: PDU,
        val resTarget: SnmpTarget,
        val resPdu: PDU,
)


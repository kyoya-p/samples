package mibtool

import kotlinx.serialization.*
import org.snmp4j.smi.Variable

@Serializable
data class VBL(
        val vbl: List<VB>
)

@Serializable
data class VB(
        val oid: String,
        val value: Variable
)


package mibtool

import kotlinx.serialization.*

@Serializable
data class SnmpParams(
        val addr: String,
        val oid: String = ".1",
        val oids: List<String> = listOf(),
        val comm: String = "public",
        val version: String = "2c"
)


package mibtool.snmp4jWrapper

import kotlinx.serialization.*

@Serializable
data class SnmpParams(
        val req: String = "walk",
        val addr: String,
        val oid: String = ".1",
        //val comm: String = "public",
        //val snmpVer: String = "1"
)


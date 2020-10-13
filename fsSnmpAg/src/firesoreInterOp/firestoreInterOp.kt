package firesoreInterOp

import fssnmpagent.AddressRange
import fssnmpagent.AgentRequest
import mibtool.SnmpConfig

fun AgentRequest.Companion.from(obj: Map<String, *>) = AgentRequest(
        addrRangeList = (obj["addrRangeList"] as List<Map<String, *>>).map { AddressRange.from(it) },
        filter = ((obj["filter"] as Map<String, *>)["oids"] as List<String>).map { it },
        report = ((obj["report"] as Map<String, *>)["oids"] as List<String>).map { it },
        snmpConfig = if (obj["snmpConfig"] == null) SnmpConfig() else SnmpConfig.from(obj["snmpConfig"] as Map<String, *>)
)

fun SnmpConfig.Companion.from(obj: Map<String, *>) = SnmpConfig(
        req = obj["req"] as String,
        comm = obj["comm"] as String,
        ver = obj["ver"] as String,
)

fun AddressRange.Companion.from(map: Map<String, *>) = AddressRange(
        type = map["type"] as String,
        addr = map["addr"] as String,
        addrEnd = map["addrEnd"] as String,
)






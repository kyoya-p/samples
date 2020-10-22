package firestoreInterOp

import AddressSpec
import AddressSpec2
import AgentRequest
import AgentRequest2
import mibtool.Credential
import mibtool.PDU

fun AgentRequest2.Companion.from(obj: Map<String?, *>) = AgentRequest2(
        addrSpecs = (obj["addrSpecs"] as List<Map<String?, *>>).map { AddressSpec2.from(it) },
        //filter = ((obj["filter"] as Map<String, *>)?.let{},
        //report = ((obj["report"] as Map<String, *>)["oids"] as List<String>).map { it },
        //snmpConfig = if (obj["snmpConfig"] == null) SnmpConfig() else SnmpConfig.from(obj["snmpConfig"] as Map<String, *>)
        autoDetectedRegister = (obj["autoDetectedRegister"] as Boolean?) ?: false
)

fun AddressSpec2.Companion.from(obj: Map<String?, *>) = AddressSpec2(
        broadcastAddr = obj["broadcastAddr"] as String?,
        unicastAddr = obj["unicastAddr"] as String?,
        unicastAddrUntil = obj["unicastAddrUntil"] as String?,
        credential = obj["credential"]?.let { Credential.from(it as Map<String?, Any?>) } ?: Credential(),
        interval = obj["interval"]?.let { it as Long } ?: 5000L,
        retries = obj["retries"]?.let { it as Int } ?: 5,
)


fun Credential.Companion.from(obj: Map<String?, *>) = Credential(
        //TODO
)




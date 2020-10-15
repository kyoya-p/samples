package firestoreInterOp

import AddressSpec
import AgentRequest
import mibtool.Credential
import mibtool.PDU

fun AgentRequest.Companion.from(obj: Map<String?, *>) = AgentRequest(
        addrSpec = AddressSpec.from(obj["addrSpec"] as Map<String?, *>),
        //filter = ((obj["filter"] as Map<String, *>)?.let{},
        //report = ((obj["report"] as Map<String, *>)["oids"] as List<String>).map { it },
        //snmpConfig = if (obj["snmpConfig"] == null) SnmpConfig() else SnmpConfig.from(obj["snmpConfig"] as Map<String, *>)
)

fun AddressSpec.Companion.from(obj: Map<String?, *>) = AddressSpec(
        broadcastAddr = obj["broadcastAddr"]?.let { it as List<String> } ?: listOf(),
        unicastAddr = obj["unicastAddr"]?.let { it as List<String> } ?: listOf(),
        unicastAddrUntil = obj["unicastAddrUntil"]?.let { it as List<String> } ?: listOf(),
        credential = obj["credential"]?.let { it as Map<String?, *>; Credential.from(it) } ?: Credential(),
        interval = obj["interval"]?.let { it as Long } ?: 5000L,
        retries = obj["retries"]?.let { it as Int } ?: 5,
)


fun Credential.Companion.from(obj: Map<String?, *>) = Credential(
        //TODO
)




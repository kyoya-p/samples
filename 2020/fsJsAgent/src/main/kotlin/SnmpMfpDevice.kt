package snmpMfpDevice

import firebaseInterOp.App
import firebaseInterOp.decodeFrom
import gdvm.agent.mib.GdvmDeviceInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import netSnmp.PDU
import netSnmp.SnmpTarget

// device/{SnmpAgent}
@Serializable
data class SnmpDevide(
    val dev: GdvmDeviceInfo,
    val type: JsonObject, // {"dev":{"mfp":{"snmp":{}}}}
    val target: SnmpTarget,
)

// device/{SnmpDevice}/query/{SnmpDevice_Query}
@Serializable
data class SnmpDevice_Query(
    val schedule: Schedule = Schedule(1),
    val pdl: PDU,
    val time: Long? = null,
)

// device/{SnmpDevice}/query/{SnmpDevice_Query}
@Serializable
data class SnmpDevice_Query_Result(
    val schedule: Schedule = Schedule(1),
    val pdl: PDU,
    val time: Long? = null,
)


/// ------
@Serializable
data class SnmpAgentQuery_Discovery(
    val scanAddrSpecs: List<SnmpTarget>,
    val autoRegistration: Boolean,
    val schedule: Schedule = Schedule(1),
    val time: Long? = null,
)

// device/{SnmpAgent}/query/{SnmpAgentQuery_DeviceBridge}
@Serializable
data class SnmpAgentQuery_DeviceBridge(
    val targets: List<SnmpDevice>,
    val time: Long? = null,
)

@Serializable
data class SnmpDevice(
    val target: SnmpTarget,
    val deviceId: String? = null, // if defined, connecet platform with this ID.
    val password: String = "#1_Sharp",
)

@Serializable
data class Schedule(
    val limit: Int = 1, //　回数は有限に。失敗すると破産するし
    val interval: Long = 0,
)

suspend fun runSnmpMfpDevice(firebase: App, deviceId: String, secret: String) {
    println("Start SNMP MFP Device ID:$deviceId    (Ctrl-C to Terminate)")

    val db = firebase.firestore()
    callbackFlow {
        db.collection("device").doc(deviceId).addSnapshotListener { docSS ->
            val snmpMfpDevice: SnmpDevide = decodeFrom<SnmpDevide>(Json {}.encodeToString(docSS))
            offer(snmpMfpDevice)
        }
        awaitClose()
    }.collectLatest { snmpDevice ->
        println("SNMP Target: ${snmpDevice.target}")
    }
}

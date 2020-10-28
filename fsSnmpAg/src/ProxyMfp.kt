package mfp.mib

import agent.mib.AppContext
import firestoreInterOp.firestoreScopeDefault
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import mibtool.*
import mibtool.snmp4jWrapper.from
import mibtool.snmp4jWrapper.sendFlow
import mibtool.snmp4jWrapper.toSnmp4j
import java.util.*


@Serializable
data class Request(
        // TODO: スケジュール設定
        val interval: Long = 1 * 60_000,
)

@Serializable
data class Report(
        val deviceId: String,
        val type: String = "mfp.mib",
        val time: Long = Date().time,
        val result: Result,
)

@Serializable
data class Result(
        val pdu: PDU,
)


@ExperimentalCoroutinesApi
suspend fun CoroutineScope.runMfp(ac: AppContext, deviceId: String, target: SnmpTarget) {
    println("Started Device ${deviceId}")
    try {
        firestoreScopeDefault { db ->
            val oids = listOf(PDU.sysName, PDU.sysDescr, PDU.sysObjectID, PDU.hrDeviceStatus, PDU.hrPrinterStatus, PDU.hrPrinterDetectedErrorState)
            val res = ac.snmp.sendFlow(
                    target = target.toSnmp4j(),
                    pdu = PDU.GETNEXT(vbl = oids.map { VB(it) }).toSnmp4j()
            ).first()

            val rep = Report(
                    deviceId = deviceId, type = "mfp.mib", time = Date().time,
                    result = Result(
                            pdu = PDU.from(res.response)
                    ),
            )

            db.collection("devLog").document().set(rep).get()
            db.collection("device").document(deviceId).set(rep).get()

//        db.firestoreDocumentFlow<Request> { collection("devConfig").document(deviceId) }.collectLatest {            println(it) }
            delay(10000)
        }
    } catch (e: Exception) {
        println("Exception in $deviceId")
        e.printStackTrace()
    } finally {
        println("Terminated Device ${deviceId}")
    }
}

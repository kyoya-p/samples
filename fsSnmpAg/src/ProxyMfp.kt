package gdvm.mfp.mib

import com.google.cloud.firestore.FirestoreOptions
import firestoreInterOp.firestoreDocumentFlow
import gdvm.agent.mib.*
import gdvm.device.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import mibtool.snmp4jWrapper.from
import mibtool.snmp4jWrapper.sendFlow
import mibtool.snmp4jWrapper.toSnmp4j
import org.snmp4j.Snmp
import org.snmp4j.transport.DefaultUdpTransportMapping
import java.util.*

val firestore = FirestoreOptions.getDefaultInstance().getService()!!
val snmp = Snmp(DefaultUdpTransportMapping().apply { listen() })

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
suspend fun runMfp(deviceId: String, password: String, target: SnmpTarget) = coroutineScope {
    println("Started Device $deviceId $target.addr:$target.port")
    try {
        // TODO: Device Authentication

        val oids = listOf(sysName, sysDescr, sysObjectID, hrDeviceStatus, hrPrinterStatus, hrPrinterDetectedErrorState)
        val res = snmp.sendFlow(
                target = target.toSnmp4j(),
                pdu = PDU(GETNEXT, vbl = oids.map { VB(it) }).toSnmp4j()
        ).first()

        val rep = Report(
                deviceId = deviceId, type = "mfp.mib", time = Date().time,
                result = Result(
                        pdu = PDU.from(res.response)
                ),
        )

        // ログと最新状態それぞれ書込み
        firestore.collection("device").document(deviceId).collection("state").document("mib").set(rep)
        firestore.collection("device").document(deviceId).collection("logs").document().set(rep)

        firestore.firestoreDocumentFlow<Request> { collection("devConfig").document(deviceId) }.collectLatest {
            // インスタンス設定に応じた処理(あれば)
            println(it)
        }
    } catch (e: Exception) {
        println("Exception in ${deviceId}/${target.addr}")
        e.printStackTrace()
    } finally {
        println("Terminated Device ${deviceId}")
    }
}

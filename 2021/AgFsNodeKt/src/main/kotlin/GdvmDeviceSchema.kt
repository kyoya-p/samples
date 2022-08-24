package gdvm.agent.mib

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

typealias GdvmTime = Long
typealias GdvmObjectType = List<String>

// /device/{GdvmGenericDevice}
@Serializable
data class GdvmGenericDevice(
    // this is abstract definition for GDVM Object
    val id: String, // same as document.id
    val cluster: String,
    val type: GdvmObjectType = listOf("dev"),
    val time: GdvmTime, // create/update time in msec from epoch
    val dev: GdvmDeviceInfo,
)

@Serializable
data class Schedule(
    val start: Int = 0, // 0 = 1970/1/1 0:00 UTC
    val interval: Int,
    val limit: Int,
)

@Serializable
data class GdvmDeviceInfo(
    val password: String = "Sharp_#1",
) {
    companion object
}

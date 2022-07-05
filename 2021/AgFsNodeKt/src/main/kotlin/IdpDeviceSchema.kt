import gdvm.agent.mib.GdvmDeviceInfo
import gdvm.agent.mib.GdvmObjectType
import gdvm.agent.mib.GdvmTime
import gdvm.agent.mib.Schedule
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class IdpScDevice(
    // device/{IdpScDevice}
    val id: String, // same as document.id
    val type: GdvmObjectType = listOf("dev", "dev.idp", "dev.idp.sc"),
    val time: GdvmTime, // create/update time in msec from epoch
    val dev: GdvmDeviceInfo,
)

@Serializable
data class IdpScDeviceQuery(
    // device/{IdpScDevice}/query/{IdpScDeviceQuery}
    val cluster: String,
    val time: Int, // Requested time
    val schedule: Schedule = Schedule(0, 0, 1),
    val commands: List<String> = listOf(),
    val resultTime: Int = 0, // Finished time, 0==Unfinished
    val result: List<String>? = null,
)




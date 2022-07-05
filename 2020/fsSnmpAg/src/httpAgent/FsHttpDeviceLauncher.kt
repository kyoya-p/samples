package httpAgent

import gdvm.device.Schedule
import kotlinx.serialization.Serializable

@Serializable
data class FsScDeviceLauncherDocument(
        val config: ScDeviceLauncherConfig
)

@Serializable
data class ScDeviceLauncherConfig(
        val schedule: Schedule = Schedule(1),
        val accessInfoList: List<AccessInfo>,
)

@Serializable
data class AccessInfo(
        val addr: String,
        val port: Int = 80,
        val login: String,
        val password: String,
)

fun main(args: Array<String>) {
    val agentId = if (args.size == 0) "httpAgent1" else args[0]

    //TODO
}

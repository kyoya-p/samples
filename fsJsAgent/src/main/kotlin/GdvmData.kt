package gdvm.agent.mib

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// /device/{GdvmGenericDevice}
@Serializable
data class GdvmGenericDevice(
    val dev: GdvmDeviceInfo,
    val type: JsonObject, //"type:{dev:{}}"
    val time: Int, // create/update time in msec from epoch
)

// /device/{GdvmGenericDevice}/query/{GdvmDeviceQuery}
@Serializable
data class GdvmDeviceQuery(
    // アプリケーション定義
    val time: Int, // create/update time in msec from epoch
)

// /device/{GdvmGenericDevice}/query/{GdvmDeviceQuery}/result/{GdvmDeviceQueryResult}
@Serializable
data class GdvmDeviceQueryResult(
    // アプリケーション定義
    val time: Int, // create/update time in msec from epoch
)

// /device/{GdvmGenericDevice}/log/{GdvmGenericLog}
@Serializable
data class GdvmGenericLog(
    // アプリケーション定義
    val type: JsonObject, //"type:{log:{}}"
    val time: Int, // create/update time in msec from epoch
)

// /group/{GdvmGenericGroup}
@Serializable
data class GdvmGenericGroup(
    val group: GdvmGroupInfo,
    val type: JsonObject, //"type:{group:{}}"
    val time: Int, // create/update time in msec from epoch
)


@Serializable
data class GdvmDeviceInfo(
    val cluster: String,
    val name: String = "anonymous",
    val password: String = "",
)

@Serializable
data class GdvmGroupInfo(
    val parent: String,
    val users: List<String>, // UserIDs
)


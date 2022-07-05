package gdvm.agent.mib

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject

typealias GdvmTime=Long
typealias GdvmObjectType=List<String>

// /device/{GdvmGenericDevice}
@Serializable
data class GdvmGenericDevice( // this is abstract definition for GDVM Object
    val id: String, // same as document.id
    val type: GdvmObjectType = listOf("dev"),
    val time: GdvmTime, // create/update time in msec from epoch
    val dev: GdvmDeviceInfo,
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
    val password: String = "Sharp_#1",
) {
    companion object
}

fun GdvmDeviceInfo.Companion.fromJson(j: JsonObject): GdvmDeviceInfo = GdvmDeviceInfo(
    cluster = j["cluster"].toString(),
    //name = j["name"] as String? ?: "noname",
    password = j["password"].toString()
)

@Serializable
data class GdvmGroupInfo(
    val parent: String,
    val users: List<String>, // UserIDs
)


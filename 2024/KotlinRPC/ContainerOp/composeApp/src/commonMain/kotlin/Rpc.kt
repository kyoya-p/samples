import io.ktor.client.*
import io.ktor.http.*
import jp.wjg.shokkaa.container.UserService
import kotlinx.datetime.Clock.System.now
import kotlinx.rpc.RemoteService
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KProperty

expect val DEV_SERVER_HOST: String
expect val DEV_SERVER_PORT: String

val client by lazy { HttpClient { installRPC() } }
suspend inline fun <reified T : RemoteService> HttpClient.service() = rpc {
    url {
        host = DEV_SERVER_HOST
        port = DEV_SERVER_PORT.toInt()
        encodedPath = "/api"
    }
    rpcConfig { serialization { json() } }
}.withService<T>()

inline fun <reified T> T.toJson(): String = Json.encodeToString(this)
inline fun <reified T> String.toObject(): T? = runCatching<T> { Json.decodeFromString(this) }.getOrNull()
fun <T> List<String>.mkItems(op: (List<String>) -> T) = drop(1).map { it.split1() }.map(op)

suspend fun UserService.getStatus() = CtStatus(
    images = ctr("i", "ls").stdout.mkItems { CtImage(this, it[0], it[1], it[2], it[3] + it[4], it[5]) },
    containers = ctr("c", "ls").stdout.mkItems { CtContainer(this, it[0], it[1]) },
    tasks = ctr("t", "ls").stdout.mkItems { CtTask(this, it[0], it[1], it[2]) }.associate { it.execId to it }
)

class CtImage(service: UserService, val id: String, type: String, digest: String, size: String, pforms: String) :
    UserService by service {
    var opts by localStorageString("containerOp.$id") { "--detach" }
    var ctrId by localStorageString("containerOp.$id")
    var args by localStorageString("containerOp.$id")

    fun String?.ifNullOrEmpty(f: () -> String) = if (isNullOrEmpty()) f() else this
    fun tmpCtrId() = ctrId.ifNullOrEmpty { "C${now().nanosecondsOfSecond % 10000}" }
    suspend fun runContainer() = ctr("run", *opts.split2(), id, tmpCtrId(), *args.split2())
    suspend fun remove() = ctr("i", "rm", id)
}

class CtContainer(service: UserService, val id: String, val imgId: String) : UserService by service {
    suspend fun remove() = ctr("c", "rm", id)
    suspend fun start() = ctr("t", "start", "-d", id)
    suspend fun killTask(signal: Int = 9) = ctr("t", "kill", "-s", "$signal", id)
    suspend fun removeTask() = ctr("t", "rm", id)
}

class CtTask(service: UserService, val execId: String, val pId: String, val status: String) : UserService by service
class CtStatus(val images: List<CtImage>, val containers: List<CtContainer>, val tasks: Map<String, CtTask>)


var pullImageId by localStorageString()
var pullImageOpts by localStorageString()

expect fun setStorage(k: String, v: String?)
expect fun getStorage(k: String): String?
class localStorageString(val appId: String = "containerOp", val defaultValue: () -> String? = { null }) {
    operator fun getValue(n: Nothing?, p: KProperty<*>) = getStorage("$appId.${p.name}") ?: defaultValue()
    operator fun setValue(n: Nothing?, p: KProperty<*>, s: String?) = setStorage("$appId.${p.name}", s)
    operator fun getValue(any: Any, p: KProperty<*>) = getStorage("$appId.${p.name}") ?: defaultValue()
    operator fun setValue(any: Any, p: KProperty<*>, s: String?) = setStorage("$appId.${p.name}", s)
}

fun String?.split1() = this?.split(Regex("\\s+"))?.filter { it.isNotEmpty() } ?: listOf()
fun String?.split2() = split1().toTypedArray()
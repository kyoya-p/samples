package jp.wjg.shokkaa.container

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.Serializable

@Rpc
interface UserService : RemoteService {
    suspend fun serverType(): String
    suspend fun ctr(vararg args: String): ProcessResult
    suspend fun unicast(request: SnmpRequest): SnmpResult
    suspend fun scan(startIp: String, endIp: String, request: SnmpRequest): Flow<SnmpResult>
}

@Serializable
data class ProcessResult(val exitCode: Int, val stdout: List<String>)

@Serializable
data class SnmpTargetV1(val adr: String, val port: Int, val community: String, val retry: Int, val intervalMs: Long)

@Serializable
open class SnmpVariable(val syntax: Int, val value: ByteArray? = null) {
    class Null : SnmpVariable(5, null)
}

@Serializable
open class SnmpVarBind(
    val strOid: String = "1.3.6",
    val oid: List<Int> = strOid.split(".").map { it.toInt() },
    val variable: SnmpVariable? = null
)

@Serializable
class SnmpRequest(
    val tgAdr: String = "127.0.0.1",
    val target: SnmpTargetV1 = SnmpTargetV1(tgAdr, 161, "public", 5, 5000),
    val pduType: Int = GETNEXT,
    val vbl: List<SnmpVarBind> = listOf()
) {
    companion object {
        val GETNEXT = -95
    }
}


@Serializable
sealed class SnmpResult(val request: SnmpRequest) {
    class Timeout(request: SnmpRequest) : SnmpResult(request)
    class Response(request: SnmpRequest) : SnmpResult(request)
}


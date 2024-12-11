package jp.wjg.shokkaa.container

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.Serializable
import kotlin.coroutines.suspendCoroutine

@Serializable
data class UserData(
    val address: String,
    val lastName: String,
)

@Rpc
interface UserService : RemoteService {
    suspend fun ctr(vararg args: String): ProcessResult
}

@Serializable
data class ProcessResult(val exitCode: Int, val stdout: List<String>, val stderr: List<String>)

//@Serializable
//data class Image(
//    val id: String,
//)
//
//@Serializable
//data class Container(
//    val id: String,
//    val imgId: String,
//)
//
//@Serializable
//data class Task(
//    val ctrId: String,
//    val pId: String,
//    val status: String,
//)

//@Serializable
//data class CtStatus(
//    val images: List<Image>,
//    val containers: List<Container>,
//    val tasks: List<Task>,
//)

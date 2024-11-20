package jp.wjg.shokkaa.container

import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RemoteService
import kotlinx.rpc.annotations.Rpc
import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val address: String,
    val lastName: String,
)

@Rpc
interface UserService : RemoteService {
    suspend fun hello(user: String, userData: UserData): String
    suspend fun subscribeToNews(): Flow<String>

    suspend fun serverType(): String
    suspend fun getStatus(): CtStatus
    suspend fun updateStatus(): Flow<CtStatus>
    suspend fun pullImage(id: String): CtStatus
    suspend fun removeImage(id: String): CtStatus
    suspend fun runContainer(imgId: String, ctrId: String, args: List<String>): CtStatus
    suspend fun removeContainer(ctrId: String): CtStatus
    suspend fun startTask(ctrId: String, args: List<String> = listOf()): CtStatus
    suspend fun execTask(ctrId: String, args: List<String> = listOf()): CtStatus
    suspend fun killTask(ctrId: String, signal: Int = 9): CtStatus

    suspend fun process(args: List<String>): String
}

@Serializable
data class Image(
    val id: String,
)

@Serializable
data class Container(
    val id: String,
    val imgId: String,
)

@Serializable
data class Task(
    val ctrId: String,
    val pId: String,
    val status: String,
)

@Serializable
data class CtStatus(
    val images: List<Image>,
    val containers: List<Container>,
    val tasks: List<Task>,
)

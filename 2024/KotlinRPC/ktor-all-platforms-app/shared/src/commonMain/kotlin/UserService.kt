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
    suspend fun status(): Flow<CtStatus>
    suspend fun pullImage(id: String): CtStatus
    suspend fun removeImage(id: String): CtStatus
    suspend fun runContainer(imgId: String, cntnrId: String, args: List<String>): CtStatus

    suspend fun process(args: List<String>): String
}

@Serializable
data class Image(
    val id: String,
)
@Serializable
data class Container(
    val id: String,
    val imageId: String,
)

@Serializable
data class CtStatus(
    val images: List<Image>,
    val containers: List<Container>,
)

/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

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
    val name: String,
)

@Serializable
data class CtStatus(
    val images: List<Image>,
)

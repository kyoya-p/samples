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
interface MyService : RemoteService {
    suspend fun version():String
    suspend fun pull(imageId: String, user: String?=null): Int
    suspend fun subscribeToNews(): Flow<ContainerStatus>
}

@Serializable
data class Image(val name: String)

@Serializable
data class Container(val name: String)

@Serializable
data class ContainerStatus(
    val images: List<Image>,
    val containers: List<Container>,
)

@Rpc
interface ContainerdService : RemoteService {
    suspend fun status(): Flow<ContainerStatus>
}
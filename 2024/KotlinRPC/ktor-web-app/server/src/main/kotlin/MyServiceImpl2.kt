/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class MyServiceImpl(override val coroutineContext: CoroutineContext) : MyService {
//    override suspend fun hello(user: String, userData: UserData): String {
//        return "Nice to meet you $user, how is it in ${userData.address}?"
//    }

//    override suspend fun subscribeToNews(): Flow<String> {
//        return flow {
//            repeat(10) {
//                delay(2000)
//                emit("Article number $it")
//            }
//        }
//    }

    override suspend fun status() = flow {
        repeat(10) {
            delay(10000)
            val r = withContext(Dispatchers.IO) {
                ProcessBuilder("ctr", "i", "ls").start().inputStream.reader().readLines()
            }
            println("[[$r]]")
            emit(ContainerStatus(images = r.map { Image(name = it) }, containers = listOf()))
        }
    }
}

//class ContainerdServiceImpl(override val coroutineContext: CoroutineContext) : ContainerdService {
//    override suspend fun status() = channelFlow {
//        repeat(10) {
//            delay(10000)
//            val r = withContext(Dispatchers.IO) {
//                ProcessBuilder("ctr", "i", "ls").start().inputStream.reader().readLines()
//            }
////            println(r)
//            trySend(ContainerStatus(images = r.map { Image(name = it) }, containers = listOf()))
//        }
//    }
//}
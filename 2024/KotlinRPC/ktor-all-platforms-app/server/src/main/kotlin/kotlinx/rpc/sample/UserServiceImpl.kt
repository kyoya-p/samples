/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.rpc.sample

import CtStatus
import Image
import UserData
import UserService
import io.ktor.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.CoroutineContext

class UserServiceImpl(override val coroutineContext: CoroutineContext) : UserService {
    override suspend fun hello(user: String, userData: UserData): String {
        return "Nice to meet you $user, how is it in ${userData.address}?"
    }

    override suspend fun subscribeToNews(): Flow<String> {
        return flow {
            repeat(10) {
                delay(300)
                emit("Article number $it")
            }
        }
    }

    override suspend fun status() = flow {
        repeat(10) {
            println(System.getProperty("os.name").toLowerCasePreservingASCIIRules())
            val cli = listOf("wsl", "--user", "root", "ctr", "i", "ls", "-q")
            val imgs = ProcessBuilder(cli).start().inputStream.reader().readLines()
            println("[[$imgs]]")
            emit(CtStatus(images = imgs.map { Image(name = it) }))
            delay(10000)
        }
    }
}

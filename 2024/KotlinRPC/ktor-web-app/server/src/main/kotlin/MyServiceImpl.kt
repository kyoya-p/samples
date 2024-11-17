/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MyServiceImpl(override val coroutineContext: CoroutineContext) : MyService {
    override suspend fun version(): String = suspendCoroutine { c ->
        val r = ProcessBuilder("ctr", "-v").start().inputStream.reader().readText().trim()
        c.resume(r)
    }

    override suspend fun pull(imageId: String, user: String?): Int = suspendCoroutine { c ->
        val r = ProcessBuilder("ctr", "i", "pull", user).start().waitFor()
        c.resume(r)
    }

    override suspend fun subscribeToNews() = flow {
        repeat(10) {
            val imgs = ProcessBuilder("ctr", "i", "ls", "-q").start().inputStream.reader().readLines()
            println("[[$imgs]]")
            emit(ContainerStatus(images = imgs.map { Image(name = it) }, containers = listOf()))
            delay(10000)
        }
    }
}


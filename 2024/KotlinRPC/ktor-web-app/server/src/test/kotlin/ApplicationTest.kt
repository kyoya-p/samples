/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import io.ktor.server.testing.*
import kotlinx.coroutines.flow.toList
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.krpc.streamScoped
import kotlinx.rpc.withService
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }

        val service = createClient {
            installRPC()
        }.rpc("/api") {
            rpcConfig {
                serialization {
                    json()
                }
            }
        }.withService<MyService>()

        assertEquals(
            expected = "ctr github.com/containerd/containerd 1.7.12",
            actual = service.version(),
        )

        streamScoped {
            assertEquals(
                expected = List(10) { ContainerStatus(listOf(Image(name = "")), listOf()) },
                actual = service.subscribeToNews().toList(),
            )
        }
    }
}

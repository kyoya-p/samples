/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package jp.wjg.shokkaa.container

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
//        val service = createClient {
//            installRPC()
//        }.rpc("/api") {
//            rpcConfig {
//                serialization {
//                    json()
//                }
//            }
//        }.withService<UserService>()

//        assertEquals(
//            expected = "Nice to meet you Alex, how is it in address1?",
//            actual = service.hello("Alex", UserData("address1", "last")),
//        )
//
//        streamScoped {
//            assertEquals(
//                expected = List(10) { "Article number $it" },
//                actual = service.subscribeToNews().toList(),
//            )
//        }
    }
}

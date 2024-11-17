/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.browser.document
import kotlinx.coroutines.launch
import kotlinx.rpc.RPCClient
//import react.create
//import react.dom.client.createRoot
//import react.useEffectOnce
//import react.useState
//import web.dom.document

import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.*
import kotlinx.rpc.krpc.ktor.client.installRPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.krpc.streamScoped
import kotlinx.rpc.withService

suspend fun main(): Unit {

    val container = document.createElement("div")
    val rpcClient = HttpClient { installRPC() }.rpc {
        url("ws://localhost:8080/api")
        rpcConfig { serialization { json() } }
    }
    val containerdSevicce = rpcClient.withService<MyService>()
    val version = containerdSevicce.version()
    document.body?.append {
        p { +"Containerd Console : $version " }
    }
    streamScoped {
        launch {
            println("Start flow : ${containerdSevicce.version()}")
            containerdSevicce.subscribeToNews().collect { c ->
                println(c)
                document.body?.append {
                    c.images.forEach { p { +"[${it.name}]" } }
                } ?: println("XXX")
            }
        }
    }

//    document.body?.append {
//        val client = HttpClient { }
//        p { +"Loading..." }
//    }

////    val app = App.create()
////    createRoot(container).render(app)
//    var rpcClient by useState<RPCClient?>(null)
//
//    useEffectOnce {
//        rpcClient = initRpcClient()
//    }
//    if (rpcClient == null) {
//        p {}
//        return
//    } else {
//        rpcClient
//    }
}

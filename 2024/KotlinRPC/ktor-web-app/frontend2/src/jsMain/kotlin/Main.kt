/*
 * Copyright 2023-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import kotlinx.rpc.RPCClient
import react.create
import react.dom.client.createRoot
import react.useEffectOnce
import react.useState
import web.dom.document

import kotlinx.html.*
import kotlinx.html.dom.append
import kotlinx.html.dom.create
import kotlinx.html.js.*

fun main() {
    val container = document.createElement("div")
    document.body.appendChild(container)

//    val app = App.create()
//    createRoot(container).render(app)
    var rpcClient by useState<RPCClient?>(null)

    useEffectOnce {
        rpcClient = initRpcClient()
    }
    if (rpcClient == null) {
    p{}
        return
    } else {
        rpcClient
                }
}

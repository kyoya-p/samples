package node.api

@JsModule("process")
@JsNonModule
external object process {
    val env: dynamic
    fun exit(code: Int)
}


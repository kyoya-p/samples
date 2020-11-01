external fun require(module: String): dynamic //javascriptのrequire()を呼ぶ

fun main() {
    val exp = require("express")
    val svr = exp()
    svr.get("/") { _, res ->
        res.send("Kotlin/JS Web Server.")
    }
    svr.listen(8080)
}


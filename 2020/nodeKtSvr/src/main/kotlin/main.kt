//  Platform: Node.js only


external fun require(module: String): dynamic

fun main(args: Array<String>) {
    val port = if (args.size != 1) 8080 else args[0].toInt()

    val express = require("express")
    val multer = require("multer")

    val app = express()
    val upload = multer(mapOf("dest" to "uploads/"))

/*    app.get("/") { req, res ->
        println("[XXXXX]")
        res.type("text/plain")
        res.send("i am a beautiful butterfly")
    }
 */
    app.post("/photos", upload.array("photos", 2)) { req, res ->
        //       val path: dynamic = req.files.recfile.path
        print(
            """
            |f: ${req.files}
            |f: ${req.files[0]}
            |f: ${req.files[0].path}
        """.trimMargin()
        )
        res.type("text/plain")
        res.send("uploaded")
    }

    app.use(express.static("./web"))
    app.listen(port) {
        println("Start Custom token generator Service. Listen port:${port}.")
    }

}


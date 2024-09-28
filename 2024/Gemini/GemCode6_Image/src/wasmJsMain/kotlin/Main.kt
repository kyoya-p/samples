//@JsModule("@google/generative-ai")
//@JsNonModule
//external class generativeAi {
//    class GoogleGenerativeAI(apiKey: String)
//}

//@JsModule("jimp")
//@JsNonModule
//external class Jimp {
//    companion object {
//        fun read(src: String, cb: (err: dynamic, image: Jimp) -> Unit): Unit
//        suspend fun read(src: String): Jimp
//    }
//}
//
//suspend fun main() {
////    val f = generativeAi.GoogleGenerativeAI("")
////    println("Hello, Kotlin/JS!")
//    val img = Jimp.read("samples/2.jpg")
//}


@JsModule("is-sorted")
@JsNonModule
external fun <T> sorted(a: Array<T>): Boolean

@JsModule("jimp")
@JsNonModule
@JsName("Jimp")
external class Jimp {
    companion object {
        suspend fun read(path: String)
    }
}

fun read(path: String) = js("Jimp.read(path)")

suspend fun main() {
    println("Is sorted: ${sorted(arrayOf(1, 2, 3))}")
    println("Is sorted: ${sorted(arrayOf(1, 3, 2))}")

    val a = read("samples/2.jpg")
}

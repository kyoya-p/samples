package genai

external interface Process {
    interface Env {
        val GOOGLE_API_KEY: String?
    }

    val args: Array<String>
    val env: Env
//    val env: Map<String, String?>
}

external val process: Process


@JsNonModule
@JsModule("@google/generative-ai")
external class GoogleGenerativeAI(apiKey:String)


fun main(args: Array<String>) {
    val apiKey = process.env.GOOGLE_API_KEY ?: throw IllegalArgumentException("Not Found: GOOGLE_API_KEY")
    println("apiKey:${apiKey}")

    val g=genai.GoogleGenerativeAI(apiKey)

}

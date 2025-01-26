suspend fun main() = appGemini()

external val process: dynamic
actual val GEMINI_API_KEY: String get() = process.env["GEMINI_API_KEY"]?: throw IllegalArgumentException()
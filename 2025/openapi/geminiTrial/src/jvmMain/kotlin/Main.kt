suspend fun main() = appGemini()

actual val GEMINI_API_KEY = System.getenv("GEMINI_API_KEY")?: throw IllegalArgumentException()
import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import kotlinx.coroutines.runBlocking

val agent = AIAgent(
    executor = simpleGoogleAIExecutor(System.getenv("GOOGLE_API_KEY")),
    llmModel = GoogleModels.Gemini2_5FlashPreview0417,
)

fun main() = runBlocking {
    val result = agent.runAndGetResult("kotlinで補完して fun fib(x)=")
    println(result)
}

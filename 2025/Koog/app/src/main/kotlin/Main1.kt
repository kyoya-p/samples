import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.model.PromptExecutorExt.execute
import kotlinx.coroutines.runBlocking

val promptExecutor = simpleGoogleAIExecutor(System.getenv("GOOGLE_API_KEY"))

fun main() = runBlocking {
    val prompt = prompt("prompt1") { user("kotlinで補完して fun fib(x)=") }
    val reply = promptExecutor.execute(prompt, GoogleModels.Gemini2_5FlashPreview0417)
    println(reply.content)
}

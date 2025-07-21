package demo1

import ai.koog.agents.core.agent.AIAgent
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor

suspend fun main() {
    val agent = AIAgent(
        executor = simpleGoogleAIExecutor(System.getenv("GOOGLE_API_KEY")),
        llmModel = GoogleModels.Gemini2_0Flash
    )
    println(agent.run("補完して fun fib(x) ="))
}

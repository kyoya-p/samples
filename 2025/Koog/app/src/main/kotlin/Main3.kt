package demo3

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import kotlinx.coroutines.runBlocking


fun main() = runBlocking {
    val toolRegistry = McpToolRegistryProvider.fromTransport(
        transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931/sse")
    )
    val agent = AIAgent(
        executor = simpleGoogleAIExecutor(System.getenv("GOOGLE_API_KEY")),
        llmModel = GoogleModels.Gemini2_0Flash,
        toolRegistry = toolRegistry,
    )

    agent.runAndGetResult("https://www.google.com で'Koog Kotlin'を検索。最初の検索結果を開く")
    generateSequence { print("> ");readlnOrNull() }.forEach { query ->
        agent.runAndGetResult(query)
    }
}


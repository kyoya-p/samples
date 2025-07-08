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

    agent.runAndGetResult("ブラウザを起動して'https://www.google.com'を開く")
    while (true) {
        print("> ")
        val query = readln()
        if (query == "q") break
        agent.runAndGetResult(query)
    }
}


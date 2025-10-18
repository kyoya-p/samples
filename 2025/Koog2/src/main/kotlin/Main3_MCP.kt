package demo3

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.McpToolRegistryProvider.fromTransport
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor

suspend fun main() {
    val toolRegistry = fromTransport(
        transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931")
    )
    val agent = AIAgent(
        executor = simpleGoogleAIExecutor(System.getenv("GOOGLE_API_KEY")),
        llmModel = GoogleModels.Gemini2_0Flash,
        toolRegistry = toolRegistry,
    )

    val res =
        agent.run("https://www.google.com で'Koog Kotlin'を検索。最初の検索結果を開き内容要約(日本語で)")
    println(res)
    generateSequence { print("> ");readlnOrNull() }.forEach { query ->
        println(agent.run(query))
    }
}


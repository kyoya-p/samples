package jp.wjg.shokkaa.mcp

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor

suspend fun createAgent() = AIAgent(
    executor = simpleGoogleAIExecutor(System.getenv("GOOGLE_API_KEY")),
    llmModel = GoogleModels.Gemini2_0Flash,
    toolRegistry = McpToolRegistryProvider.fromTransport(
        transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931")
    ),
)

fun startNodeProcess(module: String = nodeMcpServicePlaywright) = ProcessBuilder("npx", "-y", module, "--port", "8931")
    .inheritIO()
    .start()!!

val nodeMcpServicePlaywright = "@playwright/mcp@latest"
package jp.wjg.shokkaa.mcp

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor

suspend fun createAgent() = runCatching {
    AIAgent(
        executor = simpleGoogleAIExecutor(appSettings.apiKey),
        llmModel = GoogleModels.Gemini2_0Flash,
        toolRegistry = McpToolRegistryProvider.fromTransport(
            transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931")
//            transport =  SseClientTransport(
//                client = HttpClient {
//                    install(SSE)
//                    install(HttpTimeout) {
//                        connectTimeoutMillis = 60.seconds.inWholeMilliseconds
//                        socketTimeoutMillis = 60.seconds.inWholeMilliseconds
//                        requestTimeoutMillis = 60.seconds.inWholeMilliseconds
//                    }
//                },
//                urlString = "http://localhost:8931",
        ),
    )
}.onFailure { it.printStackTrace() }.getOrNull()

val nodeMcpServicePlaywright = "@playwright/mcp@latest"
fun startPlaywrightService() = startNodeProcess(
    listOf(
        "cmd.exe", "/c",
        "$nodeJsDir\\npx.cmd", "-y",
        nodeMcpServicePlaywright,
        "--host", "127.0.0.1",
        "--port", "8931"
    )
)

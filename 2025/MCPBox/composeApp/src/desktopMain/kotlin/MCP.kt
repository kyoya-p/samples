package jp.wjg.shokkaa.mcp

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor

suspend fun runAIAgent(query: String): String? = runCatching {
    AIAgent(
        executor = simpleGoogleAIExecutor(appSettings.apiKey),
        llmModel = GoogleModels.Gemini2_0Flash,
        toolRegistry = McpToolRegistryProvider.fromTransport(
            transport = McpToolRegistryProvider.defaultSseTransport("http://127.0.0.1:8931")
//            transport = SseClientTransport(
//                client = HttpClient(CIO) {
//                    install(SSE)
//                    install(HttpTimeout) {
//                        connectTimeoutMillis = 60.seconds.inWholeMilliseconds
//                    }
//                },
//                urlString = "http://localhost:8931",
//            ),
        )
    ).runAndGetResult(query)
}.onFailure { it.printStackTrace() }.getOrNull()

fun McpService.start(): Process = when (type) {
    "stdio" if command == "node" -> startNodeProcess(listOf("node") + args.split(" "))
    else -> throw Exception("not implemented")
}

suspend fun McpService.startWithEnvironment() = runCatching { start() }.getOrElse {
    runCatching {
        setupNodejsEnvironment()
        start()
    }.getOrNull()
}

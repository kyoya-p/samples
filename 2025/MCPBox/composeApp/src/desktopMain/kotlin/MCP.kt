package jp.wjg.shokkaa.mcp

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.mcp.McpToolRegistryProvider.fromTransport
import ai.koog.agents.snapshot.feature.Persistency
import ai.koog.agents.snapshot.providers.InMemoryPersistencyStorageProvider
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.io.asSink
import kotlinx.io.asSource
import kotlinx.io.buffered

suspend fun createAiAgent(process: Process) = runCatching {
    val transport = StdioClientTransport(
        process.inputStream.asSource().buffered(),
        process.outputStream.asSink().buffered(),
    )
    AIAgent(
        executor = simpleGoogleAIExecutor(appSettings.apiKey),
        llmModel = GoogleModels.Gemini2_0Flash,
        toolRegistry = fromTransport(transport)
    ) {
        install(Persistency) {
            enableAutomaticPersistency = true
            storage = InMemoryPersistencyStorageProvider(persistenceId = "storage1")
        }
    }
}.onFailure { it.printStackTrace() }.getOrThrow()

context(logger: Logger)
fun McpService.start(): Process = when (type) {
    "stdio" if command == "node" -> startNodeProcess(listOf("$nodeJsDir\\node.exe") + args.split(" "))
    else -> {
        logger.log("McpService.start().unknown type :$type, command: $command", "Application")
        throw Exception("not implemented")
    }
}

context(logger: Logger)
suspend fun McpService.startWithEnvironment(): Process? = runCatching {
    logger.log("MCPService.startWithEnvironment(): Start start()", "Application")
    start()
        .apply { logger.log("MCPService.startWithEnvironment(): Complete start()", "Application") }
}.getOrElse {
    runCatching {
        logger.log("MCPService.startWithEnvironment(): Setup Node.js environment", "Application")
        setupNodejsEnvironment()
        logger.log("MCPService.startWithEnvironment(): Start.. (retry)", "Application")
        start()
    }.onSuccess {
        logger.log("MCPService.startWithEnvironment(): Start(retry)", "Application")
    }.onFailure {
        logger.log("MCPService.startWithEnvironment(): Error: failed to start", "Application")
        it.printStackTrace()
    }.getOrNull()
}

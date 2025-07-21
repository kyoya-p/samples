package demo4

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.core.feature.handler.AgentFinishedContext
import ai.koog.agents.core.feature.handler.AgentStartContext
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.mcp.McpToolRegistryProvider.defaultSseTransport
import ai.koog.agents.mcp.McpToolRegistryProvider.fromTransport
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor

suspend fun main() {
    val promptExecutor = simpleGoogleAIExecutor(System.getenv("GOOGLE_API_KEY"))
    val agentStrategy = strategy("smart") {
        val nodeSendInput by nodeLLMRequest()
        val nodeExecuteTool by nodeExecuteTool()
        val nodeSendToolResult by nodeLLMSendToolResult()

        edge(nodeStart forwardTo nodeSendInput)
        edge((nodeSendInput forwardTo nodeFinish) transformed { it } onAssistantMessage { true })
        edge((nodeSendInput forwardTo nodeExecuteTool) onToolCall { true })
        edge(nodeExecuteTool forwardTo nodeSendToolResult)
        edge((nodeSendToolResult forwardTo nodeFinish) transformed { it } onAssistantMessage { true })
    }
    val agentConfig = AIAgentConfig(
        prompt = Prompt.build("browser-operator") { system("MCPツールを使用してブラウザを操作し、結果のみ簡潔に報告") },
        model = GoogleModels.Gemini2_0Flash,
        maxAgentIterations = 10
    )

    val agent = AIAgent(
        promptExecutor = promptExecutor,
        strategy = agentStrategy,
        agentConfig = agentConfig,
        toolRegistry = fromTransport(defaultSseTransport("http://localhost:8931")),
        installFeatures = {
            install(EventHandler) {
                onBeforeAgentStarted { eventContext: AgentStartContext<*> -> println("Starting strategy: ${eventContext.strategy.name}") }
                onAgentFinished { eventContext: AgentFinishedContext -> println("Result: ${eventContext.result}") }
            }
        }
    )

    runCatching {
        agent.run(
            "https://batspi.com/index.php?%E3%82%AB%E3%83%BC%E3%83%89%E6%83%85%E5%A0%B1%E7%B5%9E%E8%BE%BC%E3%81%BF を開く。" +
                    "'絞り込み結果'の項目名をリスト、1.ゴラドン\n" +
                    "2.ロクケラトプス" +
                    "...のように表示する。" +
                    "ページに続きがあれば、'>'ボタンで次のページにジャンプし、リスト表示を繰り返す。"
        )
    }.onFailure {
        "Error: ${it.message}"
        it.printStackTrace()
    }
}


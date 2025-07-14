package demo4

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.AIAgentStrategy
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteTool
import ai.koog.agents.core.dsl.extension.nodeLLMRequest
import ai.koog.agents.core.dsl.extension.nodeLLMSendToolResult
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onToolCall
import ai.koog.agents.features.eventHandler.feature.EventHandler
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import kotlinx.coroutines.runBlocking


fun main() = runBlocking {
    val toolRegistry = McpToolRegistryProvider.fromTransport(
        transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931")
    )

    val promptExecutor = simpleGoogleAIExecutor(System.getenv("GOOGLE_API_KEY"))
    val agentStrategy = strategy("Browser Operator") {
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
        prompt = Prompt.build("browser-operator") {
            system(
                """
                    あなたはWebブラウザのオペレータです。
                    ユーザーが入力した指示をMCPツールを使用して実行し、必要なら結果を応答。
                    例えば、特定の検索サイトを開き検索文字列を入力し結果を返す。
                    常に、結果を示す明確なメッセージで応答する。
                    可能な限りMarkdown形式で表現する。
                """.trimIndent()
            )
        },
        model = GoogleModels.Gemini2_5ProPreview0506,
        maxAgentIterations = 10
    )

    val agent = AIAgent(
        promptExecutor = promptExecutor,
        strategy = agentStrategy,
        agentConfig = agentConfig,
        toolRegistry = toolRegistry,
        installFeatures = {
            install(EventHandler) {
                onBeforeAgentStarted { strategy: AIAgentStrategy, agent: AIAgent -> println("Starting strategy: ${strategy.name}") }
                onAgentFinished { strategyName: String, result: String? -> println("Result: $result") }
            }
        }
    )

    agent.run(
        """https://batspi.com/index.php?%E3%82%AB%E3%83%BC%E3%83%89%E6%83%85%E5%A0%B1%E7%B5%9E%E8%BE%BC%E3%81%BF を開く。
           広告があればクローズ。
           ページャの[3]をクリックし3ページ目を表示。
            先頭から2番目のバトスピカード名を表示。
        """
    )
}


package demo3

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import kotlinx.coroutines.runBlocking


fun main() = runBlocking {
    val process = ProcessBuilder(
        "sudo", "docker", "run", "--rm", "-i",
        "--mount", "type=bind,src=/home/kyoya/works,dst=/projects/works",
        "mcp/filesystem", "/projects",
    ).start()

    val toolRegistry = McpToolRegistryProvider.fromTransport(
        transport = McpToolRegistryProvider.defaultStdioTransport(process)
    )
    val agent = AIAgent(
        executor = simpleGoogleAIExecutor(System.getenv("GOOGLE_API_KEY")),
        llmModel = GoogleModels.Gemini2_0Flash,
        toolRegistry = toolRegistry,
    )

    val result =
        agent.runAndGetResult("ルートディレクトリ以下にあるすべてのファイルを列挙")
    println(result)
}


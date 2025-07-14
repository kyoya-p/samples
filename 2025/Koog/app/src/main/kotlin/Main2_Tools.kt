import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

@Suppress("unused")
@LLMDescription("OSのFile system 操作")
class MyTools : ToolSet {
    @Tool
    @LLMDescription("pathで指定されたディレクトリ内のファイルを列挙。ファイル名,タイプ(true:Directory,false:File),サイズ, の配列を返す。")
    fun listFiles(
        @LLMDescription("列挙するディレクトリを指定") path: String
    ) = SystemFileSystem.run {
        list(Path(path)).joinToString("\n") { "${it.name}, " + metadataOrNull(it)?.run { "$isDirectory, $size" } }
    }
}

fun main() = runBlocking {
    val agent = AIAgent(
        executor = simpleGoogleAIExecutor(System.getenv("GOOGLE_API_KEY")),
        llmModel = GoogleModels.Gemini2_0Flash,
        toolRegistry = ToolRegistry { tools(MyTools().asTools()) },
    )
    val result =
        agent.runAndGetResult("MDで出力: ディレクトリapp/src/main/kotlin以下にあるファイルリストと、それらのサイズ合計表示")
    println(result)
}


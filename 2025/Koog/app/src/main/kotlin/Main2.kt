import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString

@Suppress("unused")
@LLMDescription("OSのFile system 操作")
class MyTools : ToolSet {
    @Tool
    @LLMDescription("pathで指定されたディレクトリ内のファイルを列挙。ファイル名,タイプ(D:Directory,F:File),サイズ, の配列を返す。")
    fun listFiles(
        @LLMDescription("列挙するディレクトリを指定") path: String
    ) = with(SystemFileSystem) {
        list(Path(path)).filter { it.name != "." && it.name != ".." }.joinToString("\n") {
            "${it.name}, ${if (metadataOrNull(it)?.isDirectory == true) "D" else "F"}, ${metadataOrNull(it)?.size}"
        }
    }

    @Tool
    @LLMDescription("pathで指定したファイルの内容を返す")
    fun readFile(
        @LLMDescription("対象ファイルを指定") path: String
    ) = with(SystemFileSystem) { source(Path(path)).buffered().readString() }
}

val agent = AIAgent(
    executor = simpleGoogleAIExecutor(System.getenv("GOOGLE_API_KEY")),
    llmModel = GoogleModels.Gemini2_0Flash,
    toolRegistry = ToolRegistry { tools(MyTools().asTools()) },
)

fun main() = runBlocking {
        val result = agent.runAndGetResult("ディレクトリapp/src/main/kotlin以下にあるすべてのファイルの内容からファイル操作を行うものを列挙")
    println(result)
}


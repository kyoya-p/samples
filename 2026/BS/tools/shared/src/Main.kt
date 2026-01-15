import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.charleskorn.kaml.Yaml
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlinx.io.buffered

val defaultCachePath = Path(getEnv("USERPROFILE").ifEmpty { getEnv("HOME") }.ifEmpty { "." }, ".bscards")

class SearchCards : CliktCommand("バトルスピリッツ カード検索 CLI") {
    init {
        context {
            helpOptionNames = setOf("-h", "--help")
            helpFormatter = { context -> MordantHelpFormatter(context, showDefaultValues = true) }
        }
    }

    //    override val printHelpOnEmptyArgs = true
    val keywords by argument(help = "検索キーワード（フリーワード）").multiple(required = false)
    val force by option("-f", "--force", help = "キャッシュが存在する場合でも強制的に再取得して上書き").flag()
    val cacheDir by option("-d", "--cache-dir", help = "カードデータのキャッシュ先ディレクトリを指定", envvar = "BSCARD_CACHE_DIR")
        .convert { Path(it) }.default(defaultCachePath)
    val cost by option("-c", "--cost", help = "コスト範囲を指定（例: '3-5'、'7'）。").default("0-30")
    val attributes by option("-a", "--color", "--attr", help = "属性（色）を指定（例: 赤, 紫）。複数回指定でOR検索").multiple()
    val attributeMode by option("-m", "--color-mode", "--attr-mode", help = "属性指定時の検索モード（AND/OR）を指定").default("OR")
    val categories by option("-t", "--type", "--category", help = "カードカテゴリを指定（例: スピリット, アルティメット）").multiple()
    val systems by option("-s", "--system", "--family", help = "系統を指定します（例: 星竜, 勇傑）。複数回指定でモードに従い検索").multiple()
    val systemMode by option("-n", "--system-mode", "--family-mode", help = "系統指定時の検索モード（AND/OR）を指定").default("OR")
    val blockIcons by option("-b", "--block", help = "ブロックアイコンの番号を指定（例: 7）。複数指定可。").multiple()

    override fun run() {
        runBlocking {
            val (costMin, costMax) = cost.split("-").map { it.trim().toInt() }.let { if (it.size == 1) it + it else it }
            if (!SystemFileSystem.exists(cacheDir)) SystemFileSystem.createDirectories(cacheDir)

            fun <T, E> Flow<T>.distinctBy(op: (T) -> E): Flow<T> = flow {
                val seen = mutableSetOf<E>()
                collect { value -> if (seen.add(op(value))) emit(value) }
            }
            println("Freewords: $keywords & Cost: $costMin-$costMax ")

            createClient().use { client ->
                bsSearchMain(
                    client = client,
                    keywords = keywords.joinToString(" "),
                    cardNo = "", // todo
                    costMin = costMin,
                    costMax = costMax,
                    attributes = attributes,
                    categories = categories,
                    systems = systems,
                    blockIcons = blockIcons,
                    attributeSwitch = attributeMode,
                    systemSwitch = systemMode,
                ).distinctBy { it.cardNo }.collectIndexed { ix, searched ->
                    val fn = Path(cacheDir, "${searched.cardNo}.yaml")
                    print("$ix: target: ${searched.cardNo} : ")
                    if (force || !SystemFileSystem.exists(fn)) {
                        val card = bsDetail(client, searched.cardNo)
                        SystemFileSystem.sink(fn).buffered()
                            .use { it.writeString(Yaml.default.encodeToString(card)) }
                        println("collected $fn .")
                    } else println("already exists.")
                }
            }
        }
    }
}

fun main(args: Array<String>) = SearchCards().main(args)

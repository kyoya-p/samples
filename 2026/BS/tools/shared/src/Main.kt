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
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlinx.io.buffered

val defaultCachePath = Path(getEnv("USERPROFILE").ifEmpty { getEnv("HOME") }.ifEmpty { "." }, ".bscards")

class SearchCards : CliktCommand("Battle Spirits Cards Search CLI") {
    init {
        context {
            helpOptionNames = setOf("-h", "--help")
            helpFormatter = { context ->
                MordantHelpFormatter(context, showDefaultValues = true)
            }
        }
    }

    override val printHelpOnEmptyArgs = true
    val keywords by argument(help = "Search keywords").multiple(required = false)
    val force by option("-f", "--force", help = "Force rewrite cache").flag()
    val cacheDir by option("-c", "--cache-dir", help = "Cache Directory", envvar = "BSCARD_CACHE_DIR")
        .convert { Path(it) }.default(defaultCachePath)
    val cost by option("--cost", help = "Cost range (e.g. '3-5' or '7')").default("0-30")

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
                    attr = "", // todo
                    category = emptyList(),  // todo
                    system = emptyList(),    // todo
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

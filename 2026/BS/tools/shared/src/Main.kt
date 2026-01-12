import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.writeString
import kotlinx.io.buffered

class SearchCards : CliktCommand("Battle Spirits Cards Search CLI") {
    private val keywords by argument(help = "Search keywords").multiple(required = false)

    override fun run() {
        runBlocking {
            val cacheDirStr = getEnv("BSCARD_CACHE_DIR").ifEmpty {
                val home = getEnv("USERPROFILE").ifEmpty { getEnv("HOME") }
                "$home/.bscards"
            }

            val cacheDirPath = Path(cacheDirStr)
            if (!SystemFileSystem.exists(cacheDirPath)) SystemFileSystem.createDirectories(cacheDirPath)

            val keywordStr = keywords.joinToString(" ")

            fun <T, E> Flow<T>.distinctBy(op: (T) -> E): Flow<T> = flow {
                val seen = mutableSetOf<E>()
                collect { value -> if (seen.add(op(value))) emit(value) }
            }
    println("Freewords: $keywords")

            bsSearchMain(
                keywords = keywordStr,
                cardNo = "",
                costMin = 0,
                costMax = 30,
                attr = "",
                category = emptyList(),
                system = emptyList()
            ).distinctBy { it.cardNo }.collect { searched ->
                val fn = Path(cacheDirPath, "${searched.cardNo}.yaml")
                print("target: $fn : ")
                if (!SystemFileSystem.exists(fn)) {
                    val card = bsDetail(searched.cardNo)
                    SystemFileSystem.sink(fn).buffered().use { it.writeString(Yaml.default.encodeToString(card)) }
                    println("collected.")
                } else println("already exists.")
            }
        }
    }
}

fun main(args: Array<String>) = SearchCards().main(args)

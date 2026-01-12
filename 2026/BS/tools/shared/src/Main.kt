import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.*
import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.io.buffered

@OptIn(ExperimentalCoroutinesApi::class)
fun main(args: Array<String>) = runBlocking {
    val cacheDirStr = getEnv("BSCARD_CACHE_DIR").ifEmpty {
        val home = getEnv("USERPROFILE").ifEmpty { getEnv("HOME") }
        "$home/.bscards"
    }

    val cacheDirPath = Path(cacheDirStr)
    if (!SystemFileSystem.exists(cacheDirPath)) SystemFileSystem.createDirectories(cacheDirPath)

    val keywords = args.joinToString(" ")
    if (keywords.isEmpty()) {
        println("Usage: <Keywords>")
        return@runBlocking
    }

    fun <T, E> Flow<T>.distinctBy(op: (T) -> E): Flow<T> = flow {
        val seen = mutableSetOf<E>()
        collect { value -> if (seen.add(op(value))) emit(value) }
    }

    bsSearchMain(
        keywords = keywords,
        cardNo = "",
        costMin = 0,
        costMax = 30,
        attr = "",
        category = emptyList(),
        system = emptyList()
    ).distinctBy { it.cardNo }.collect { searched ->
        val fn = Path(cacheDirPath, "${searched.cardNo}.yaml")
        if (!SystemFileSystem.exists(fn)) {
            val card = bsDetail(searched.cardNo)
            SystemFileSystem.sink(fn).buffered().use { it.writeString(Yaml.default.encodeToString(card)) }
        } else println("exist cache: $fn")
    }
}


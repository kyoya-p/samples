import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.*
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readString
import kotlinx.io.writeString
import kotlinx.io.buffered

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

    bsSearchMain(
        keywords = keywords,
        cardNo = "",
        costMin = 0,
        costMax = 30,
        attr = "",
        category = emptyList(),
        system = emptyList()
    ).collect { searchedCard ->
        val cardNo = searchedCard.cardNo
        val cacheFilePath = Path(cacheDirPath, "$cardNo.yaml")

        val cards: List<Card> = if (SystemFileSystem.exists(cacheFilePath)) {
            val yamlText = SystemFileSystem.source(cacheFilePath).buffered().use { it.readString() }
            Yaml.default.decodeFromString(yamlText)
        } else {
            println("[Web] Fetching $cardNo")
            val webCards = bsDetail(cardNo).toList()
            val yamlText = Yaml.default.encodeToString(webCards)
            SystemFileSystem.sink(cacheFilePath).buffered().use { it.writeString(yamlText) }
            webCards
        }

        cards.forEach { card ->
            println(card)
        }
    }
}

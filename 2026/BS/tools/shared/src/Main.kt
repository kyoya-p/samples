import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.*
import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

fun main(args: Array<String>) = runBlocking {
    val cacheDir = getEnv("BSCARD_CACHE_DIR").ifEmpty {
        val home = getEnv("USERPROFILE").ifEmpty { getEnv("HOME") }
        "$home/.bscards"
    }
    

    
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
        val cachePath = "$cacheDir/$cardNo.yaml"
        
        val cards: List<Card> = if (fileExists(cachePath)) {
            // println("[Cache] Loading $cardNo")
            val yamlText = readFileText(cachePath)
            Yaml.default.decodeFromString(yamlText)
        } else {
            println("[Web] Fetching $cardNo")
            val webCards = bsDetail(cardNo).toList()
            val yamlText = Yaml.default.encodeToString(webCards)
            writeFileText(cachePath, yamlText)
            webCards
        }
        
        cards.forEach { card ->
            println(card)
        }
    }
}
import kotlinx.coroutines.runBlocking

fun main(args: Array<String>) = runBlocking {
    val cacheDir = getEnv("BSCARD_CACHE_DIR")
    val keywords = args.joinToString(" ")
    bsSearchMain(
        keywords = keywords,
        cardNo = "",
        costMin = 0,
        costMax = 30,
        attr = "",
        category = emptyList(),
        system = emptyList()
    ).collect { searchedCard ->
        bsDetail(searchedCard.cardNo).collect { card ->
            println(card)
        }
    }
}

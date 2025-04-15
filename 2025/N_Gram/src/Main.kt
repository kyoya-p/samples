fun generateNGrams(text: String, n: Int): List<String> {
    val ngrams = mutableListOf<String>()
    for (i in 0..text.length - n) {
        ngrams.add(text.substring(i, i + n))
    }
    return ngrams
}

//fun generateNGrams(text: String, n: Int) = text.windowed(n)
    
fun search(query: String, documents: List<String>, n: Int): List<String> {
    val queryNGrams = generateNGrams(query, n)
    return documents.filter { document ->
        val documentNGrams = generateNGrams(document, n)
        queryNGrams.all { ngram ->
            documentNGrams.contains(ngram)
        }
    }
}

fun main() {
    val documents = listOf(
        "今日はいい天気です。",
        "明日は雨が降るでしょう。",
        "昨日は晴れていました。"
    )

    val query = "天気"
    val n = 2

    val results = search(query, documents, n)
    println(results) // [今日はいい天気です。]
}
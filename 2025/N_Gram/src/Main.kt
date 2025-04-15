fun search(db: List<Record>, query: String, n: Int) {
    val queryNGrams = query.windowed(n) // N-Gram生成
    println("Query: $query($queryNGrams)")
    db.forEach { record ->
        println(record.ngrams)
        if (queryNGrams.all { ngram -> record.ngrams.contains(ngram) }) {
            println("=> ${record.text}")
        }
    }
    println()
}

class Record(val text: String, val n: Int, val ngrams: List<String> = text.windowed(n))

fun main() {
    val n = 3
    val db = listOf(
        Record("今日はいい天気です。", n),
        Record("明日は雨が降るでしょう。", n),
        Record("昨日は晴れていました。", n),
        Record("明日の天気予報では雪になりそう", n),
        Record("明日の天気は雪の予報です", n),
    )

    search(db, "天気", n)
    search(db, "天気予報", n)
}
import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readLine


class Record(val text: String, val n: Int, val ngrams: List<String> = text.windowed(n))

fun main() = with(SystemFileSystem) {
    val texts = (readLines("SampleData_ja.txt")).joinToString("\n")
    for (i in 1..20) {
        makeNgrams(texts, i..i).let { println("$i: ${it.size}") }
    }
    //    ngramMatchingSearch(records, "革新的な再", n)
}

fun makeNgrams(text: String, nRange: IntRange) = nRange.asSequence().flatMap { n -> text.windowed(n) }.toSet()

fun ngramMatchingSearch(db: Collection<Record>, query: String, n: Int) {
    val queryNGrams = query.windowed(n) // N-Gram生成
    println("Query: $query($queryNGrams)")
    db.filter { record -> queryNGrams.all { ngram -> record.ngrams.contains(ngram) } }.forEach {
        println("  => ${it.text.take(64)}...")
    }
}


fun FileSystem.readLines(file: String) = source(Path(file)).buffered().let { generateSequence { it.readLine() } }


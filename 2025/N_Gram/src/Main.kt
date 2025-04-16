import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readLine

val ngramIndexes = mutableMapOf<String, Set<Int>>()

class Record(val text: String, val n: Int, val ngrams: List<String> = text.windowed(n))

fun main() = with(SystemFileSystem) {
    val n = 2
    val records = readLines("SampleData_ja.txt").toList().map { Record(it, n) }
    records.forEachIndexed { ix, r -> r.ngrams.forEach { ngramIndexes[it] = (ngramIndexes[it] ?: emptySet()) + ix } }
    println("ngramIndexes:${ngramIndexes.size}")
    ngramMatchingSearch(records, "革新的な再", n)
}

fun ngramMatchingSearch(db: Collection<Record>, query: String, n: Int) {
    val queryNGrams = query.windowed(n) // N-Gram生成
    println("Query: $query($queryNGrams)")
    db.filter { record -> queryNGrams.all { ngram -> record.ngrams.contains(ngram) } }.forEach {
        println("  => ${it.text.take(64)}...")
    }
    println()
}

fun indexSearch(index: Map<String, Set<Int>>, query: String, n: Int) {
    val queryNGrams = query.windowed(n) // N-Gram生成
    println("Query: $query($queryNGrams)")
    db.filter { record -> queryNGrams.all { ngram -> record.ngrams.contains(ngram) } }.forEach {
        println("  => ${it.text.take(64)}...")
    }
    println()
}


fun FileSystem.readLines(file: String) = source(Path(file)).buffered().let { generateSequence { it.readLine() } }


import kotlinx.io.buffered
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readLine

fun main(): Unit = with(SystemFileSystem) {
    val R = 1..2
    val texts = (readLines("SampleData_ja.txt") + readLines("SampleData_en.txt")).toList()

    val ngramIndexes: Map<String, Set<Int>> = texts.mapIndexed { ix, tx -> makeNgrams(tx, R).map { it to ix } }
        .flatten().groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }

    ngramIndexSearch(ngramIndexes, makeNgrams("革新的な再", R)).forEach {
        println("[$it] ${texts[it].take(64)}")
    }
}

fun makeNgrams(text: String, nRange: IntRange) = nRange.flatMap { n -> text.windowed(n).distinct() }.toSet()

fun ngramIndexSearch(nGramIdx: Map<String, Set<Int>>, qNgrams: Set<String>): Set<Int> {
    val q = qNgrams.map { nGramIdx[it] ?: return@map emptySet() }
    return q.drop(1).fold(q.first()) { a, e -> a.intersect(e) }
}

fun FileSystem.readLines(file: String) = source(Path(file)).buffered().let { generateSequence { it.readLine() } }


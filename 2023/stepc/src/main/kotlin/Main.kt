import java.io.File

fun main(args: Array<String>) {
    val tgDir = File(args.getOrNull(0) ?: ".").absoluteFile
    val grList = args.drop(1).asSequence().flatMap { File(it).readLines() }.map { it.split(",") }
    val grMap = grList.sortedBy { -it[1].length }.associate { tgDir.resolve(it[1]) to it[0] }
    val counters = mutableMapOf<String, Int>()
    tgDir.walk().filter { it.isFile && it.extension.lowercase() == "txt" }.forEach { f ->
        val gr = grMap.entries.first { (k, _) -> f.startsWith(k) }.value
        counters[gr] = (counters[gr] ?: 0) + f.readLines().size
    }
    println(counters)
}

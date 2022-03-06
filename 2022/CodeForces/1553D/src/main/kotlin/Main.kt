fun main() {
    val keys = readln()
    keys.forEach { println("[$it]") }
    val line = readln()

    sequenceBackspace(keys, "", "").forEach {
        println(it)
    }
}

fun sequenceBackspace(keys: String, line: String, s: String): Sequence<String> = when {
    keys.isEmpty() -> sequenceOf(line)
    else ->
        sequenceBackspace(keys.drop(1), line + keys.take(1), s)  + sequenceBackspace(keys.drop(1), line.dropLast(1), s)
}

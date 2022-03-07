fun main() {
    val keys = readln()
    val target = readln()

    sequenceBackspace(keys, "", target).filter { it == target }.forEach {
        println(it)
    }
}

fun sequenceBackspace(keys: String, line: String, target: String): Sequence<String> = when {
    keys.isEmpty() -> sequenceOf(line)
    !canEnterString(line + keys, target) -> sequenceOf()
    else ->
        sequenceBackspace(keys.drop(1), line + keys.take(1), target) + sequenceBackspace(keys.drop(1),
            line.dropLast(1),
            target)
}

fun canEnterString(keys: String, target: String): Boolean {
    val km = keys.groupingBy { it }.eachCount()
    val tm = target.groupingBy { it }.eachCount()
    return tm.all { (ch, c) -> (km[ch] ?: 0) >= c }.also { println("($keys,$target):$it") }
}
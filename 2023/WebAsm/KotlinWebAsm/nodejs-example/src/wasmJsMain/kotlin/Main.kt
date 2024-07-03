import kotlin.time.measureTime

fun main() {
    val p=measureTime {
        for (i in 1..100000L) {
            collatz(i).count()
        }
    }
    println(p)
}

fun collatz(x: Long) = generateSequence(x) { e -> if (e % 2 == 0L) (e / 2) else (e * 3 + 1) }.takeWhile { it != 1L }

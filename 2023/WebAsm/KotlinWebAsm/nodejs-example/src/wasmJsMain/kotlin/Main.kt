fun main() {
    for (i in 1..1000L) {
        print("$i:")
        collatz(i).forEach { print("*") }
        println()
    }
}

fun fib() = generateSequence(1 to 1) { (a, b) -> a + b to a }
fun collatz(x: Long) = generateSequence(x) { e -> if (e % 2 == 0L) (e / 2) else (e * 3 + 1) }.takeWhile { it != 1L }

fun main() {
    println("Hello")
    fib(1, 1).take(10).forEach {
        println(it)
    }
}

fun fib(a1: Int, a2: Int) = generateSequence(a1 to a2) { (a1, a2) -> a2 to a1 + a2 }
        .map { it.second }


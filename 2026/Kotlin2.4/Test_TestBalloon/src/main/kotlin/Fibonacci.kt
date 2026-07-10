fun fib(n: Int): Int {
    require(n >= 0) { "n must be non-negative" }
    if (n == 0) return 0
    if (n == 1) return 1
    return fib(n - 1) + fib(n - 2)
}

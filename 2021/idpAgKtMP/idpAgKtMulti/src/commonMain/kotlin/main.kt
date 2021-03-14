fun main() {
    var list = listOf(1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21)
        .filter {it > 5}
        .map {it * 2}
        .groupBy {it % 5}
    println(list)
}

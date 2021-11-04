import java.util.*

fun main() {
    val si = Scanner(System.`in`)
    val width = si.nextInt()
    val height = si.nextInt()
    val field = List(height) { List(width) { si.nextInt() } }

    val max = (0 until height).maxOf { y ->
        (0 until width).maxOf { x ->
            val candyColor = field[y][x]
            fun countCandy(pred: (Int) -> Boolean) = generateSequence(1) { it + 1 }.takeWhile { pred(it) }.count()
            val r = countCandy { x + it < width && field[y][x + it] == candyColor }
            val d = countCandy { y + it < height && field[y + it][x] == candyColor }
            val ld = countCandy { y + it < height && x - it >= 0 && field[y + it][x - it] == candyColor }
            val rd = countCandy { y + it < height && x + it < width && field[y + it][x + it] == candyColor }
            listOf(r, d, ld, rd).maxOf { it }
        }
    }
    println("max=$max")
}
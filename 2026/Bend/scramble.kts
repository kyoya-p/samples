// 🤖[AI生成: IntRange を重複なしでランダム順に返す遅延 Sequence(O(1) メモリ)]
import kotlin.random.Random

/**
 * 範囲の全要素を重複なく scramble した Sequence を返す。
 * 2^k 周期の LCG(Hull–Dobell: c 奇数, a ≡ 1 mod 4)を 1 周させ、範囲外の値はスキップ。
 * 暗号用途の乱数品質ではない。
 */
fun IntRange.scrambled(random: Random = Random.Default): Sequence<Int> {
    if (isEmpty()) return emptySequence()
    val n = last.toLong() - first + 1
    val m = 1L shl (64 - java.lang.Long.numberOfLeadingZeros(n - 1))
    val mask = m - 1
    val a = ((random.nextLong() shl 2) + 1) and mask
    val c = (random.nextLong() or 1L) and mask
    val start = random.nextLong() and mask
    return generateSequence(start) { ((a * it + c) and mask).takeIf { x -> x != start } }
        .filter { it < n }
        .map { (first + it).toInt() }
}

val r = 1..10
val s = r.scrambled().toList()
println(s)
check(s.sorted() == r.toList())
println((-3..3).scrambled(Random(42)).joinToString())
println((0 until 1_000_000).scrambled().take(5).toList())

// 🤖[AI生成: Feistel 置換 + cycle walking で IntRange を重複なしランダム順に返す遅延 Sequence(O(1) メモリ)]
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * 範囲の全要素を重複なく scramble した Sequence を返す。
 * [0, 2^(2h)) 上の平衡 Feistel(ラウンド関数 F_i(R) = HMAC-SHA256(key, i‖R))で置換 E を作り、
 * cycle walking(E を [0, n) に入るまで繰り返す)で [0, n) 上の置換に制限する。
 * F が PRF なら 3 ラウンドで PRP、4 ラウンドで強 PRP(Luby–Rackoff)。識別優位の上界は概ね q²/2^h で、
 * 小さい範囲(h が小さい)では保証にならない。XOR 型 Feistel は常に偶置換(Even–Goldreich 1983)で、
 * 小さい n ではラウンド数を増やしても一様にならない(randtest.main.kts T1/T4)。
 * 全単射性・停止性・出力が置換であることは proof/Feistel.lean で証明。
 */
fun IntRange.scrambledFeistel(
    key: ByteArray = ByteArray(32).also { SecureRandom().nextBytes(it) },
    rounds: Int = 4,
): Sequence<Int> {
    if (isEmpty()) return emptySequence()
    val n = last.toLong() - first + 1
    val h = (64 - java.lang.Long.numberOfLeadingZeros(n - 1) + 1) / 2
    val mask = (1L shl h) - 1
    val mac = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(key, "HmacSHA256")) }
    val buf = ByteArray(9)
    fun f(i: Int, r: Long): Long {
        buf[0] = i.toByte()
        for (b in 0 until 8) buf[1 + b] = (r ushr (8 * b)).toByte()
        val d = mac.doFinal(buf)
        var v = 0L
        for (b in 0 until 8) v = v or ((d[b].toLong() and 0xFF) shl (8 * b))
        return v and mask
    }
    fun e(x: Long): Long {
        var y = x
        for (i in 0 until rounds) y = ((y and mask) shl h) or ((y ushr h) xor f(i, y and mask))
        return y
    }
    return (0L until n).asSequence().map { i ->
        var x = e(i)
        while (x >= n) x = e(x)
        (first + x).toInt()
    }
}

val r = 1..10
val s = r.scrambledFeistel().toList()
println(s)
check(s.sorted() == r.toList())
println((-3..3).scrambledFeistel(ByteArray(32) { it.toByte() }).joinToString())
println((0 until 1_000_000).scrambledFeistel().take(5).toList())
check((0 until 100_000).scrambledFeistel().toSet().size == 100_000)

// 🤖[AI生成: scramble.kts(LCG)と feistel.kts(Feistel + cycle walking)の統計検定。χ² 検定と NIST SP 800-22 の frequency / runs 検定。棄却基準 α = 0.01]
@file:Import("scramble.kts")
@file:Import("feistel.kts")

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

// ---- 数値関数 ----

/** 相補誤差関数(Numerical Recipes erfcc、相対誤差 < 1.2e-7) */
fun erfc(x: Double): Double {
    val z = abs(x)
    val t = 1 / (1 + 0.5 * z)
    val ans = t * exp(-z * z - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 +
        t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 +
        t * (-0.82215223 + t * 0.17087277)))))))))
    return if (x >= 0) ans else 2 - ans
}

/** ln Γ(x)(Lanczos 近似) */
fun lnGamma(xx: Double): Double {
    val cof = doubleArrayOf(76.18009172947146, -86.50532032941677, 24.01409824083091,
        -1.231739572450155, 0.1208650973866179e-2, -0.5395239384953e-5)
    var y = xx
    val tmp = xx + 5.5 - (xx + 0.5) * ln(xx + 5.5)
    var ser = 1.000000000190015
    for (c in cof) { y += 1; ser += c / y }
    return -tmp + ln(2.5066282746310005 * ser / xx)
}

/** 正則化上側不完全ガンマ関数 Q(a, x)(級数 / 連分数) */
fun igamc(a: Double, x: Double): Double {
    if (x <= 0) return 1.0
    val front = exp(-x + a * ln(x) - lnGamma(a))
    if (x < a + 1) {
        var ap = a
        var del = 1 / a
        var sum = del
        for (i in 1..10_000) {
            ap += 1; del *= x / ap; sum += del
            if (abs(del) < abs(sum) * 1e-15) break
        }
        return 1 - sum * front
    }
    val tiny = 1e-300
    var b = x + 1 - a
    var c = 1 / tiny
    var d = 1 / b
    var hh = d
    for (i in 1..10_000) {
        val an = -i * (i - a)
        b += 2
        d = an * d + b; if (abs(d) < tiny) d = tiny
        c = b + an / c; if (abs(c) < tiny) c = tiny
        d = 1 / d
        val del = d * c
        hh *= del
        if (abs(del - 1) < 1e-15) break
    }
    return front * hh
}

/** χ² 統計量 → p 値 */
fun chiSquareP(stat: Double, df: Int) = igamc(df / 2.0, stat / 2)

/** NIST SP 800-22 2.1 Frequency (Monobit) Test */
fun monobitP(bits: BooleanArray): Double {
    val s = bits.sumOf { if (it) 1L else -1L }
    return erfc(abs(s) / sqrt(bits.size.toDouble()) / sqrt(2.0))
}

/** NIST SP 800-22 2.3 Runs Test(前提の frequency 条件を満たさなければ 0) */
fun runsP(bits: BooleanArray): Double {
    val n = bits.size.toDouble()
    val pi = bits.count { it } / n
    if (abs(pi - 0.5) >= 2 / sqrt(n)) return 0.0
    val v = 1 + (1 until bits.size).count { bits[it] != bits[it - 1] }
    return erfc(abs(v - 2 * n * pi * (1 - pi)) / (2 * sqrt(2 * n) * pi * (1 - pi)))
}

fun bitsOf(s: String) = BooleanArray(s.length) { s[it] == '1' }
fun fmt(p: Double) = "%.6f".format(p) + if (p < 0.01) " 棄却" else ""

// ---- 自己検査: NIST SP 800-22 の例題値と χ²(df=2)の閉形式 ----

println("== 自己検査 ==")
val selfMono = monobitP(bitsOf("1011010101"))
val selfRuns = runsP(bitsOf("1001101011"))
val selfChi = chiSquareP(5.0, 2)
println("monobit 1011010101 = %.6f(NIST 例 0.527089)".format(selfMono))
println("runs    1001101011 = %.6f(NIST 例 0.147232)".format(selfRuns))
println("χ²(5, df=2)       = %.6f(exp(-2.5) = %.6f)".format(selfChi, exp(-2.5)))
check(abs(selfMono - 0.527089) < 1e-6 && abs(selfRuns - 0.147232) < 1e-6 && abs(selfChi - exp(-2.5)) < 1e-9)

// ---- 検定対象 ----

val gens: List<Pair<String, (IntRange, Long) -> Sequence<Int>>> = listOf(
    "LCG" to { r, seed -> r.scrambled(Random(seed)) },
    "Feistel4" to { r, seed -> r.scrambledFeistel(Random(seed).nextBytes(32), rounds = 4) },
    "Feistel8" to { r, seed -> r.scrambledFeistel(Random(seed).nextBytes(32), rounds = 8) },
    "Feistel16" to { r, seed -> r.scrambledFeistel(Random(seed).nextBytes(32), rounds = 16) },
)

// ---- T1: 置換の一様性(n = 5、全 120 通りの出現頻度) ----

println("\n== T1: 置換の一様性 n=5, 試行 60000, df=119 ==")
for ((name, g) in gens) {
    val trials = 60_000
    val cells = 120
    val counts = HashMap<List<Int>, Long>()
    for (seed in 0L until trials) counts.merge(g(0..4, seed).toList(), 1, Long::plus)
    val e = trials.toDouble() / cells
    val stat = counts.values.sumOf { (it - e) * (it - e) / e } + (cells - counts.size) * e
    println("%-8s 出現した置換 %3d / %d  p = %s".format(name, counts.size, cells, fmt(chiSquareP(stat, cells - 1))))
}

// ---- T2: 位置 × 値 の一様性(n = 10) ----

println("\n== T2: 位置×値の一様性 n=10, 試行 50000, df=81 ==")
for ((name, g) in gens) {
    val n = 10
    val trials = 50_000
    val counts = Array(n) { LongArray(n) }
    for (seed in 0L until trials) g(0 until n, seed).forEachIndexed { pos, v -> counts[pos][v]++ }
    val e = trials.toDouble() / n
    val stat = counts.sumOf { row -> row.sumOf { (it - e) * (it - e) / e } }
    println("%-8s p = %s".format(name, fmt(chiSquareP(stat, (n - 1) * (n - 1)))))
}

// ---- T4: 置換の偶奇(n = 16 = 2^(2h) で cycle walking なし。一様なら偶置換率 0.5) ----

println("\n== T4: 偶置換率 n=16, 試行 20000 ==")
fun isEven(p: List<Int>): Boolean {
    var inv = 0
    for (i in p.indices) for (j in i + 1 until p.size) if (p[i] > p[j]) inv++
    return inv % 2 == 0
}
for ((name, g) in gens) {
    val trials = 20_000
    val ev = (0L until trials).count { isEven(g(0 until 16, it).toList()) }
    // 二項分布の正規近似で両側 p 値
    val z = (ev - trials / 2.0) / sqrt(trials / 4.0)
    println("%-9s 偶置換率 %.4f  p = %s".format(name, ev.toDouble() / trials, fmt(erfc(abs(z) / sqrt(2.0)))))
}

// ---- T3: NIST frequency / runs(出力の最下位ビット列、n = 1,000,000) ----

println("\n== T3: NIST SP 800-22 最下位ビット列 n=1000000 ==")
for ((name, g) in gens) {
    for (seed in 0L until 3) {
        val bits = g(0 until 1_000_000, seed).map { it and 1 == 1 }.toList().toBooleanArray()
        println("%-8s seed=%d  frequency p = %s  runs p = %s".format(name, seed, fmt(monobitP(bits)), fmt(runsP(bits))))
    }
}

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.security.MessageDigest
import java.util.*
import kotlin.concurrent.thread


fun main() {
    // スレッド
    val start1 = Date().time
    val tg1 = (1..20).map { i ->
        thread(start = true) {
            load1(i)
        }
    }
    tg1.forEach { it.join() }
    println()
    println(Date().time - start1)

    // コルーチン
    // - 非同期タスクをたくさん呼び出すのには良い
    // - メニーコアを活用する役には立たない
    val start2 = Date().time
    runBlocking {
        (1..20).forEach { i ->
            load2(i)
        }
        println(Date().time - start2)
    }
    println()
    println(Date().time - start2)
}

fun load1(i: Int) {
    val sha512 = MessageDigest.getInstance("SHA-512")
    val src = "1234567890abcdef".toByteArray()
    print("st:$i ")
    for (i in 0..99999 * 5) {
        sha512.digest(src)
    }
    println("en:$i ")
}

suspend fun load2(i: Int) {
    val sha512 = MessageDigest.getInstance("SHA-512")
    val src = "1234567890abcdef".toByteArray()
    print("st:$i ")
    for (i in 0..99999 * 5) {
        yield()
        sha512.digest(src)
    }
    println("en:$i ")
}

import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.time.ExperimentalTime


@ExperimentalTime
@Suppress("NonAsciiCharacters")
class `T13-Coroutine_Cancelation` {

    @Test
    fun `t01-コルーチンのキャンセル`(): Unit = runBlocking {
        var counter = 0
        val job = async { // 100msに1ずつカウントアップ
            assertThrows<CancellationException> { // cancel()された場合例外が発生する。必要ならcatchして終了処理
                while (isActive) { // while(true)の代わりにwhile(isActive)でループ
                    delay(100) // suspend関数の入口(コンテキストスイッチ)でcancel()される
                    yield() // 呼ぶべきsuspend関数がない場合は適切にyield()でコンテキストスイッチを発生させる
                    counter++
                }
            }
        }
        delay(350) // カウントが3に上がったころに..
        job.cancel() // ..中断
        delay(300)
        assert(counter == 3)

        // cancel()後のawait()は例外を発生させる
        assertThrows<CancellationException> {
            job.await() //例外
        }
    }

    @Test
    // 親コルーチンがキャンセルされたら子コルーチンもキャンセルされる
    fun `t02-launchされたコルーチン`(): Unit = runBlocking {
        var res = ""
        val j = launch {
            launch { delay(100); res += "2" }
            launch { delay(50); res += "1" }
            delay(30)
            res += "0"
        }
        delay(70)
        j.cancel()
        delay(70)
        println(res)
        assert(res == "01")
    }

    @Test
    // コンテキストを共有する子コルーチンがまとめてキャンセルされる
    // launch{}とかはデフォルトで親のコンテキストを共有する
    fun `t03-コルーチンのコンテキスト`(): Unit = runBlocking {
        val jobHandlerEven = Job()
        val jobHandlerOdd = Job()

        var res = ""
        suspend fun f(i: Int) {
            delay(i * 10L)
            res += "$i"
        }
        for (i in 0..9) {
            if (i % 2 == 0) launch(jobHandlerEven) { f(i) }
            else launch(jobHandlerOdd) { f(i) }
        }
        delay(45)
        jobHandlerOdd.cancel() // 奇数番目のjobをまとめてキャンセル
        delay(100)
        println(res)
        assert(res == "0123468")
    }
}

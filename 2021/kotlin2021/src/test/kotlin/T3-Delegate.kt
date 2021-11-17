import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import kotlinx.datetime.Clock.System.now
import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@Suppress("NonAsciiCharacters")
class `T3-Delegate` {
    @Test
    fun `t01-lazy遅延初期化`() {
        var c = 3

        val d by lazy { c } // immutableな変数

        println(d)
        assert(d == 3)
        c = 5
        println(c)
        assert(d == 3)
    }

    @Test
    fun `t02-デリゲートの定義_関数デリゲート`() {
        class Mod5(private var a: Int = 0) {
            operator fun getValue(thisRef: Any?, property: KProperty<*>) = a
            operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                a = value % 5
            }
        }

        var a by Mod5()
        a = 4
        assert(a == 4)
        a = a + 1
        assert(a == 0)
    }

    @ExperimentalTime
    @Test
    fun `t03-デリゲートの定義_クラスデリゲート`(): Unit = runBlocking {
        class ThrottledQueue<E>(
            val inner: Channel<E> = Channel(Channel.RENDEZVOUS),
        ) : Channel<E> by inner {
            val n by lazy { now() }
            override suspend fun receive(): E {

                delayUntilNextPeriod(milliseconds(30), start = n)
                return inner.receive()
            }
        }

        val que = ThrottledQueue<Int>()
        stopWatch { w ->
            launch {
                runCatching {
                    while (isActive) {
                        val r = que.receive()
                        println("${w.now()} $r")
                    }
                }
            }

            for (i in 1..5) {
                que.send(i)
            }
            que.close()
        }
    }

}

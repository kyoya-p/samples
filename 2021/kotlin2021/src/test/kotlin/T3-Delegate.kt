import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty

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
    fun `t02-デリゲートの定義`() {
        class Mod5 {
            var a = 0
            operator fun getValue(thisRef: Any?, property: KProperty<*>): Int {
                return a
            }

            operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                a = value % 5
            }
        }

        var a by Mod5()
        a = 3
        assert(a == 3)
        a = a + a
        assert(a == 1)
    }
}

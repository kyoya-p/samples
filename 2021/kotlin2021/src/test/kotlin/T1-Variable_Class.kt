import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty

@Suppress("NonAsciiCharacters")
class `T1-Variable_Class` {
    @Test
    fun `t01-変数`() {
        @Suppress("UNUSED_VARIABLE")
        val v = 1 // immutableな変数
        // v++ // syntax error

        var c = 1 // mutableな変数
        c++
        println(c)
    }

    @Test
    fun `t02-クラスと継承`() {
        class C(val a: Int)

        val v = C(1)

        // class Sub() : C // syntax error: 基底クラスはopenで無ければならない
        open class Base(val a: Int)
        class Sub(a: Int) : Base(a) {}

    }

    @Test
    fun `t11-関数型を既定クラスに`() { // Kotlin 1.6~
        class MyFunc : (Int) -> Int {
            override fun invoke(p1: Int) = p1 + 1
        }

        val f = MyFunc()
        println(f(2))
        assert(f(2) == 3)
    }
}

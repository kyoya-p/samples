import org.junit.jupiter.api.Test
import kotlin.reflect.KProperty

@Suppress("NonAsciiCharacters")
class `T1-Variable_Class` {
    @Test
    fun `t01-変数`() {
        val v = 1 // immutableな変数
        // v++ // syntax error

        var c = 1 // mutableな変数
        c++
        println(c)
    }

    @Test
    fun `t02-クラス`() {
        class C(val a: Int)

        val v = C(1)

        // class Sub() : C // syntax error: 基底クラスはopenで無ければならない
        open class Base(val a: Int)
        class Sub(a: Int) : Base(a) {}
        
    }
}

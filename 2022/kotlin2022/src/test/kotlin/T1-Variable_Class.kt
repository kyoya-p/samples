import org.junit.jupiter.api.Test

@Suppress("NonAsciiCharacters")
class `T1-Variable_Class` {
    @Test
    fun `t01-変数`() {
        @Suppress("UNUSED_VARIABLE")
        val v = 1 // immutableな変数
        // v++ // syntax error

        var c = 1 // mutableな変数
        c++
        assert(c == 2)
    }

    @Suppress("UNUSED_VARIABLE", "unused")
    @Test
    fun `t02-クラス`() {
        class C0 // 空のクラス

        val c0 = C0()

        class C1(val v: Int) // プライマリコンストラクタでメンバ定数の宣言と初期化

        val c1 = C1(1)

        class C2<T>(var rs: Iterable<T>) {
            init { //初期化ブロック
                rs = rs.reversed()
            }
        }

        val c2 = C2("123".toList())
        assert(c2.rs == "321".toList())

        class C3(val f: Float) {
            constructor(i: Int) : this(i.toFloat()) //セカンダリコンストラクタ
        }

        val c3 = C3(5)
        assert(c3.f == 5.toFloat())
    }

    @Suppress("unused")
    @Test
    fun `t03-クラスと継承`() {
        @Suppress("UNUSED_VARIABLE")
        class C(val a: Int)

        @Suppress("UNUSED_VARIABLE")
        val v = C(1)

        // class Sub() : C // syntax error: 基底クラスはopenで無ければならない
        open class Base(val a: Int)

        @Suppress("UNUSED_VARIABLE")
        class Sub(a: Int) : Base(a)
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

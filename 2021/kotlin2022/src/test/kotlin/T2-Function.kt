import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test

@Suppress("NonAsciiCharacters", "TestFunctionName", "ClassName")
class `T2-Function` {
    @Test
    fun t1_引数を与える値を返す関数() {
        fun add(a: Int, b: Int): Int {
            return a + b
        }

        assert(add(4, 5) == 9)
    }

    @Test
    fun t2_拡張メソッド() {
        fun Int.add(o: Int): Int {
            return this + o
        }

        assert(4.add(5) == 9)
    }

    @Test
    fun t3_中置記法() {
        infix fun Int.addOp(o: Int): Int {
            return this + o
        }

        assert(4 addOp 5 == 9)
    }

    @Test
    fun t4_オペレータオーバーロード() {
        operator fun String.times(t: Int): String {
            return (0 until t).fold("") { a, _ -> a + this }
        }

        assert("4" * 5 == "44444")
    }

    @Test
    fun t5_式表現で関数定義() {
        fun add(a: Int, b: Int) = a + b

        assert(add(4, 5) == 9)
    }

    class A
    class B {
        companion object //静的拡張関数のためにこの行が必要
    }  // Javaクラスに対しては companion object が用意できず不可能

    @Test
    fun t21_静的拡張メソッド() {
        @Suppress("unused")
        fun A.func() {
            println("A.func()")
        }
        A().func()

        fun B.Companion.func() {
            println("B.Companion.func()")
        }

        B.func()
    }

    private val time get() = Clock.System.now().toString()
    private var io
        get() = readln()
        set(s) = println(s)

    @Test
    fun t22_getterSetter() {
        io = time
    }
}

import org.junit.jupiter.api.Test

@Suppress("NonAsciiCharacters")
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
}
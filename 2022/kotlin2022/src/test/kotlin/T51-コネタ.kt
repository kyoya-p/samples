import org.junit.jupiter.api.Test


@Suppress("ClassName", "NonAsciiCharacters", "TestFunctionName")
class `T51-コネタ` {
    @Test
    fun 式の途中にデバッグ表示を挟む() {
        val res = "hello [shokkaa]!!"
            .dropWhile { it != '[' }.drop(1)
            .takeWhile { it != ']' }
            .also(::println) // <=
            .let { "<$it>" }
        println(res)
    }
}

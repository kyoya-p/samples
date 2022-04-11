@file:Suppress("UNUSED_VARIABLE")

import org.junit.jupiter.api.Test


@Suppress("ClassName", "NonAsciiCharacters", "TestFunctionName")
class V1_6_20 {
    @Test
    fun nullを許容しないジェネリクス型() { // Ktlin 1.6.20~
        fun <T> nullable(x: T): T = x
        fun <T> nonnullable(x: T & Any): T & Any = x

        val n1: String = nullable("NonNull")
        val n2: Nothing? = nullable(null)

        val nn1: String = nonnullable("NonNull")
        // val nn2 :Nothing? = nonnullable(null) // Compile Error
    }
}

import org.junit.jupiter.api.Test

@Suppress("NonAsciiCharacters")
class `T3-Exception` {
    @Suppress("UNREACHABLE_CODE")
    @Test
    fun t1_runCatching() {
        var assert = ""
        val r = runCatching {
            throw Exception("e1.")
            assert += "success"
            "s1."
        }.onFailure { ex1 ->
            // passed
            println(ex1.message)
            assert += ex1.message
        }.recoverCatching { ex1 ->
            // passed
            println(ex1.message)
            assert += ex1.message
            throw Exception("e2.")
            assert += "s2."
            "s2."
        }.onFailure { ex2 ->
            // passed
            println(ex2.message)
            assert += ex2.message
        }.onSuccess {
            assert += "s3."
        }.getOrNull()

        assert(r == null)
        assert(assert =="e1.e1.e2.")
    }
}

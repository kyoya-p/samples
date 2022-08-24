import org.junit.jupiter.api.*

@DisplayName("テストクラス")
class `T0-TestTemplate` {
    @Suppress("unused")
    companion object {

        @DisplayName("テストの最初に1回だけ実行されるメソッド")
        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            println("afterAll()")
        }

        @DisplayName("テストの最後に1回だけ実行されるメソッド")
        @JvmStatic
        @AfterAll
        fun afterAll() {
            println("afterAll()")
        }
    }

    @DisplayName("テストメソッドの前に実行されるメソッド")
    @BeforeEach
    fun beforeEach() {
        println("beforeEach()")
    }

    @DisplayName("テストメソッドの後に実行されるメソッド")
    @AfterEach
    fun afterEach() {
        println("afterEach()")
    }

    @DisplayName("テストメソッド(1)")
    @Test
    fun test1() {
        println("test1()")
        assert(1 + 2 == 3)
    }

    @DisplayName("テストメソッド(2)")
    @Test
    fun test2() {
        println("test2()")
        assert(1 + 2 == 3)
    }
}

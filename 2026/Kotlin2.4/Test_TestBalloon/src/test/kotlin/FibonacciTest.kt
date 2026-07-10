import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe

val FibonacciTests by testSuite {
    test("fibonacci base cases") {
        fib(0) shouldBe 0
        fib(1) shouldBe 1
    }

    test("fibonacci sequence values") {
        fib(2) shouldBe 1
        fib(3) shouldBe 2
        fib(4) shouldBe 3
        fib(5) shouldBe 5
        fib(6) shouldBe 8
        fib(7) shouldBe 13
        fib(8) shouldBe 21
        fib(9) shouldBe 34
        fib(10) shouldBe 55
    }

    test("fibonacci negative input throws exception") {
        shouldThrow<IllegalArgumentException> {
            fib(-1)
        }
    }
}

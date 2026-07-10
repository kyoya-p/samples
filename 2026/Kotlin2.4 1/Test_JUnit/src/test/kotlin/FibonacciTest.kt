package jvm.fibonacci

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FibonacciTest {

    @Test
    fun testFibonacciBaseCases() {
        assertEquals(0, fib(0))
        assertEquals(1, fib(1))
    }

    @Test
    fun testFibonacciSequence() {
        assertEquals(1, fib(2))
        assertEquals(2, fib(3))
        assertEquals(3, fib(4))
        assertEquals(5, fib(5))
        assertEquals(8, fib(6))
        assertEquals(13, fib(7))
        assertEquals(21, fib(8))
        assertEquals(34, fib(9))
        assertEquals(55, fib(10))
    }

    @Test
    fun testFibonacciNegativeInput() {
        assertThrows(IllegalArgumentException::class.java) {
            fib(-1)
        }
    }
}

package flow.scope3

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.*

@ExperimentalCoroutinesApi
fun main(): Unit = runBlocking {
    val j1 = launch {
        srcFlow(3, 200).collectLatest { a ->
            try {
                srcFlow(3, 100).collect { b ->
                    aTask(a * 100 + b)
                }
            } finally {
                println("term srcFlow()")
            }
        }
    }
    reset()
    delay(700)
    j1.cancel()

    val j2 = launch {
        srcFlow(3, 200).collectLatest { a ->
            try {
                srcFlow(3, 100).collect { b ->
                    aTask(a * 100 + b)
                }
            } finally {
                println("term srcFlow()")
            }
        }
    }
    reset()
    delay(700)
    j2.cancel()
}

var start = 0L
fun reset() {
    start = Date().time
}

fun rap() = "000${(Date().time - start) % 10000}".takeLast(4)

fun aTask(id: Int) {
    try {
        repeat(5) { j ->
            val x = if (j == 4) "x" else if (j == 0) "v" else ""
            println("${rap()} task=$id : $j $x")
            delay(100)
        }
    } finally {
        println("Term. task=$id")
    }
}

@ExperimentalCoroutinesApi
fun srcFlow(times: Int, inteval: Long) = callbackFlow {
    repeat(times) {
        offer(it)
        delay(inteval)
    }
    close()
    awaitClose()
}


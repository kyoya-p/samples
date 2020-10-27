import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

fun main(): Unit = runBlocking {
    println("sample1")
    flow {
        repeat(5) {
            emit(it)
            delay(100)
        }
    }.collect { println(it) }

    println("sample2")
    val j1 = launch {
        flow {
            repeat(5) {
                emit(it)
                delay(100)
            }
        }.collect { println(it) }
    }
    delay(200)
    j1.cancel()

    println("sample3")
    val j3 = launch {
        flow {
            emit(5)
        }.collect {
            repeat(it) {
                println(it)
                delay(100)
            }
        }
    }
    delay(200)
    j3.cancel()

    println("sample4")
    val j4 = launch {
        flow {
            flow {
                emit(5)
            }.collect {
                emit(50)
            }
        }.collect {
            repeat(5) {
                println(it)
                delay(100)
            }
        }
    }
    delay(200)
    j4.cancel()

}



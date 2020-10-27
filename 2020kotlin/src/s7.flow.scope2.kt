import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

@ExperimentalCoroutinesApi
fun main(): Unit = runBlocking {
    println("sample1-----")
    srcFlow(5).toAdd(3).collect { println(it) }

    println("sample2-----")
    val j1 = launch { srcFlow(5).toAdd(3).collect { println(it) } }
    delay(200)
    j1.cancel()

    println("sample3-----")
    val j3 = launch {
        srcFlow(1).toAdd(3).collect { r ->
            launch {
                repeat(r) { i ->
                    println("$r $i")
                    delay(100)
                }
            }
        }
    }
    delay(200)
    j3.cancel()

    println("sample4-----")
    launch {
        srcFlow(3).toAdd(1).toTimes(100).rescheduleFlow(5).collect {
            println(it)
        }
    }
}

@ExperimentalCoroutinesApi
fun srcFlow(r: Int) = callbackFlow {
    repeat(r) {
        offer(it)
        delay(100)
    }
    close()
    awaitClose()
}

@ExperimentalCoroutinesApi
fun Flow<Int>.toAdd(x: Int) = channelFlow {
    this@toAdd.collect {
        offer(it + x)
    }
    close()
    awaitClose()
}

@ExperimentalCoroutinesApi
fun Flow<Int>.toTimes(x: Int) = channelFlow {
    this@toTimes.collect {
        offer(it * x)
    }
    close()
    awaitClose()
}

@ExperimentalCoroutinesApi
fun Flow<Int>.rescheduleFlow(x: Int) = channelFlow {
    this@rescheduleFlow.collectLatest { sc ->
        launch {
            for (i in 0..x) {
                offer(sc + i)
                delay(100)
            }
        }
    }
    close()
    awaitClose()
}


package flow.scope3

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import java.util.*

@ExperimentalCoroutinesApi
fun main(): Unit = runBlocking {

    println("Sample1-----")
    // 多重Flowから生成されたサブScopeも外部からのキャンセルで停止する
    // ただし、collectLatestによる中断ではサブScopeはキャンセルされない
    //   collectLatestの押し出しにより中断された古いcollectブロックから派生したScopeを停止するには?
    val j1 = launch {
        srcFlow(3, 100).collectLatest { a ->
            launch {
                srcFlow(5, 100).collectLatest { b ->
                    launch {
                        aTask(a * 10 + b, 5, 100)
                        //println("${rap()} ---  Term. inner $a $b")
                    }.join()
                }
                //println("${rap()} ---  Term. outer $a")
            }.join()
        }
    }
    reset()
    delay(600)
    j1.cancel()
    j1.join()

    println("Sample2-----")
    // collectLatestの押し出しにより中断された古いcollectブロックから派生したScopeを停止するには?
    reset()
    launch {
        srcFlow(3, 200).collectLatest { a ->
            val j = launch {
                (0..2).forEach {
                    launch {
                        aTask(a * 10 + it, 1, 500)
                    }
                }
            }
            try {
                j.join()
            } finally {
                j.cancel() //終了時(停止含む)に中で生成したScopeをcancel()
            }
        }

    }
}

var start = 0L
fun reset() {
    start = Date().time
}

fun rap() = "000${(Date().time - start) % 10000}".takeLast(4)

suspend fun aTask(id: Int, times: Int, interval: Long) {
    try {
        println("${rap()} ---  Start. task=$id")
        repeat(times) { j ->
            val x = if (j == times - 1) "x" else if (j == 0) "v" else ""
            //println("${rap()} task=$id : $j $x")
            delay(interval)
        }
    } finally {
        println("${rap()} ---  Term. task=$id")
    }
}

@ExperimentalCoroutinesApi
fun srcFlow(times: Int, inteval: Long) = flow<Int> {
    repeat(times) {
        emit(it)
        delay(inteval)
    }
}


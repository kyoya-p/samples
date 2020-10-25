import kotlinx.coroutines.*

fun main(): Unit = runBlocking {
    val j1 = launch {
        val j2 = launch {
            repeat(10) { i ->
                delay(100)
                println("j2: $i")
            }
            println("Cmpl. (2)") // 停止するので到達しない
        }
        val j3 = launch {
            try {
                repeat(10) { i ->
                    delay(100)
                    println("j3: $i")
                }
            } finally {
                println("Cmpl. (3) in finally block") // 停止するが終了処理だけは実行したい場合
            }
        }
        j2.join()
        j3.join()
        println("Cmpl. (1)") // 停止するので到達しない
    }
    delay(500)
    j1.cancel()
}


import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow

@OptIn(DelicateCoroutinesApi::class)
suspend fun main() {
    GlobalScope.launch {
        val jA = launch { greeting("A") }
        launch { greeting("B") }
        delay(300)
        jA.cancelAndJoin()
    }
}

suspend fun greeting(name: String) {
    flow {
        for (i in 1..5) {
            emit(i)
            delay(200)
        }
    }.collect {
        println("[$name: $it]")
    }
}

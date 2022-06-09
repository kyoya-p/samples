import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow

@OptIn(DelicateCoroutinesApi::class)
suspend fun main() {
    GlobalScope.launch {
        val j = launch { greeting("World1") }
        launch { greeting("World2") }
        delay(300)
        j.cancelAndJoin()
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

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlin.time.Duration.Companion.seconds

suspend fun appMain() {
    flowOf(1, 2, 3).onEach { delay(1.seconds) }.collect {
        println(it)
    }
}

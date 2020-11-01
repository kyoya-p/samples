import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*

@InternalCoroutinesApi
suspend fun main() {
    flowOf("Hello,", "frends", ".").onEach { delay(1000) }.collect { print(it) }
}


import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class GreetingTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun testGreeting() = runTest {
        launch { scan("A") }
        launch { scan("B") }
    }
}
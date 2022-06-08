import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay

 fun main() = CoroutineScope(Dispatchers.Default){
    val job = launch { delay(1000) }
    delay(500)
    println(greeting("Ag_FireS_KtJsNode"))
}

fun greeting(name: String) =
    "Hello, $name"
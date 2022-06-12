import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import java.util.*

fun main() {
    function1(5+10)
    5.add(10)
    5 addOp 10
}


fun function1(i: Int) {
    println(i)
}

fun Int.add(i: Int) {
    println(this + i)
}

infix fun Int.addOp(b: Int) {
    println(this + b)
}

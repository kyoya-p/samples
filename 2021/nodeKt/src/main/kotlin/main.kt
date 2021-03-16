external val process: dynamic
val args: Array<String> get() = process.argv

fun main() {
    println("Hello: args= $args")
}

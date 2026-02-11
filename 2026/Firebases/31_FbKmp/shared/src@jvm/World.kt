actual fun getWorld(): String = "JVM World"

actual fun startFtxuiLoop(renderer: () -> String) {
    println("JVM: Simulated loop")
    println(renderer())
}

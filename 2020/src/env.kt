import com.sun.jna.platform.win32.Kernel32Util

fun main() {

    val kernel32 = Kernel32.createInstance()
    kernel32.SetEnvironmentVariableA("X", "XXX")
    println("X=" + System.getenv("X"))
    println(Kernel32Util.getEnvironmentVariable("X"))

    Kernel32.createInstance().SetEnvironmentVariableA("X", "YYY")
    println("X=" + System.getenv("X"))
    println(Kernel32Util.getEnvironmentVariable("X"))

}

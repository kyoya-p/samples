import com.sun.jna.platform.win32.Kernel32Util

// Sample: JNA
interface Kernel32 : com.sun.jna.Library {
    fun Sleep(dwMilliseconds: Int) //Sample:
    fun SetEnvironmentVariableA(name: String, value: String): Boolean //Sample:

    companion object {
        fun createInstance() = com.sun.jna.Native.loadLibrary("kernel32", Kernel32::class.java) as Kernel32
    }
}

fun main() {
    // Sample: JNA Kernel32 Utility for win32
    Kernel32Util.getEnvironmentVariables().forEach { k, v -> println("$k=$v") }

    // sample: JNA call
    val kernel32 = Kernel32.createInstance()
    kernel32.Sleep(1000)
}

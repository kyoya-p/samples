import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test


class `T41-JNA` {
    // build.gradle.kts:
    // dependancies{
    //    implementation("net.java.dev.jna:jna:5.9.0")
    //    implementation("net.java.dev.jna:jna-platform:5.9.0")
    // }

    fun now() = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        .run { "%4d%02d%02d.%03dZ".format(hour, minute, second, nanosecond / 1000 / 1000) }


    interface Kernel32 : Library {
        fun Sleep(dwMilliseconds: Int)
        fun MessageBoxA(hWnd: Pointer?, lpText: String?, lpCaption: String?, uType: Int): Int

        companion object {
            val instance by lazy { Native.load("kernel32", Kernel32::class.java) as Kernel32 }
        }
    }

    @Test
    fun t1_jna1() {
        println("${now()}: started")
        Kernel32.instance.Sleep(1000)
        println("${now()}: finished")
    }

    @Test
    fun t2_msgbox() {
        Kernel32.instance.MessageBoxA(null, "Test", "Caption", 0)
        //TODO Error
    }
}

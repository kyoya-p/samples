import com.sun.jna.Library
import com.sun.jna.Native
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test


class `T41-jna` {
    // build.gradle.kts:
    // dependancies{
    //    implementation("net.java.dev.jna:jna:5.9.0")
    //    implementation("net.java.dev.jna:jna-platform:5.9.0")
    // }

    fun now() = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        .run { "%4d%02d%02d.%03dZ".format(hour, minute, second, nanosecond/1000/1000) }

    @Test
    fun jna1() {
        println("${now()}: started")
        Kernel32.INSTANCE.Sleep(1000)
        println("${now()}: finished")
    }
    interface Kernel32 : Library {
        fun Sleep(dwMilliseconds: Int)

        companion object {
            val INSTANCE = Native.load("kernel32", Kernel32::class.java) as Kernel32
        }
    }
}

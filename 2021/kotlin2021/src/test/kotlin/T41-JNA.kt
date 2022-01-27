import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
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
        // com.sun.jna.platform.win32 に定義済み
        fun Sleep(dwMilliseconds: Int)
        fun GetComputerNameW(lpBuffer: CharArray?, lpnSize: IntByReference?): Boolean
        fun GetComputerNameA(lpBuffer: ByteArray?, lpnSize: IntByReference?): Boolean

        companion object {
            val instance by lazy { Native.load("kernel32", Kernel32::class.java) }
        }
    }

    @Test
    fun t1_値渡し() {
        println("${now()}: started")
        Kernel32.instance.Sleep(1000)
        println("${now()}: finished")
    }

    interface User32 : Library {
        fun MessageBoxA(hWnd: Pointer?, lpText: String?, lpCaption: String?, uType: Int): Int
        fun GetCursorPos(lpPoint: com.sun.jna.platform.win32.WinDef.POINT?): Boolean

        companion object {
            val instance by lazy { Native.load("user32", User32::class.java) }
        }
    }

    @Test
    fun t2_msgbox() {
        //User32.instance.MessageBoxA(null, "テスト", "Caption", 0)
        // 開いたダイアログをクローズできない...
    }

    interface IpHlpAPI : Library {
        fun GetInterfaceInfo(hWnd: Pointer?, lpText: String?, lpCaption: String?, uType: Int): Int

        companion object {
            val instance by lazy { Native.load("iphlpapi", IpHlpAPI::class.java) }
        }
    }

    @Test
    fun t3_参照渡し() {
        //TODO
        val lenComputerName: IntByReference = IntByReference()
        Kernel32.instance.GetComputerNameW(null, lenComputerName)
        val computerNameW = CharArray(lenComputerName.getValue())
        Kernel32.instance.GetComputerNameW(computerNameW, lenComputerName)
        println(String(computerNameW).dropLast(1)) // 終端の`0u0000`を削除

        Kernel32.instance.GetComputerNameA(null, lenComputerName)
        val computerNameA = ByteArray(lenComputerName.getValue())
        Kernel32.instance.GetComputerNameA(computerNameA, lenComputerName)
        println(String(computerNameA).dropLast(1)) // 終端の`0x00`を削除


    }

    @Test
    fun t4_構造体渡し() {
        // https://qiita.com/everylittle/items/b888cbec643f14de5ea6
        // WindowsAPIは、 com.sun.jna.platform.win32 にいくつかは定義済み
        val pos = com.sun.jna.platform.win32.WinDef.POINT()
        User32.instance.GetCursorPos(pos)
        println(pos.x)
        println(pos.y)
    }
}

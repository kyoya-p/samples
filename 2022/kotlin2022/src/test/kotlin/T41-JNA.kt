@file:Suppress("NonAsciiCharacters", "TestFunctionName")

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.ptr.IntByReference
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.jupiter.api.Test


@Suppress("ClassName")
class `T41-JNA` {
    // build.gradle.kts:
    // dependencies{
    //    implementation("net.java.dev.jna:jna:5.9.0")
    //    implementation("net.java.dev.jna:jna-platform:5.9.0")
    // }

    private fun now() = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        .run { "%4d%02d%02d.%03dZ".format(hour, minute, second, nanosecond / 1000 / 1000) }


    interface Kernel32 : Library {
        // com.sun.jna.platform.win32 に定義済み
        fun Sleep(dwMilliseconds: Int)
        fun GetComputerNameW(lpBuffer: CharArray?, lpnSize: IntByReference?): Boolean
        fun GetComputerNameA(lpBuffer: ByteArray?, lpnSize: IntByReference?): Boolean

        companion object {
            val instance: Kernel32? by lazy { Native.load("kernel32", Kernel32::class.java) }
        }
    }

    @Test
    fun t1_値渡し() {
        println("${now()}: started")
        Kernel32.instance!!.Sleep(1000)
        println("${now()}: finished")
    }

    interface User32 : Library {
        @Suppress("unused")
        fun MessageBoxA(hWnd: Pointer?, lpText: String?, lpCaption: String?, uType: Int): Int
        fun GetCursorPos(lpPoint: com.sun.jna.platform.win32.WinDef.POINT?): Boolean

        companion object {
            val instance: User32? by lazy { Native.load("user32", User32::class.java) }
        }
    }

    @Test
    fun t2_MessageBox() {
        //User32.instance.MessageBoxA(null, "テスト", "Caption", 0)
        // 開いたダイアログをクローズできない...
    }

    @Test
    fun t3_参照渡し() {
        val k32 = Kernel32.instance!!
        val lenComputerName = IntByReference()
        k32.GetComputerNameW(null, lenComputerName)
        val computerNameW = CharArray(lenComputerName.value)
        k32.GetComputerNameW(computerNameW, lenComputerName)
        println(String(computerNameW).dropLast(1)) // 終端の`0u0000`を削除

        k32.GetComputerNameA(null, lenComputerName)
        val computerNameA = ByteArray(lenComputerName.value)
        k32.GetComputerNameA(computerNameA, lenComputerName)
        println(String(computerNameA).dropLast(1)) // 終端の`0x00`を削除


    }

    @Test
    fun t4_構造体渡し() {
        // https://qiita.com/everylittle/items/b888cbec643f14de5ea6
        // WindowsAPIは、 com.sun.jna.platform.win32 にいくつかは定義済み
        val pos = com.sun.jna.platform.win32.WinDef.POINT()
        User32.instance!!.GetCursorPos(pos)
        println(pos.x)
        println(pos.y)
    }

    abstract class IP_ADAPTER_INDEX_MAP : Structure() {
        @Suppress("SpellCheckingInspection")
        override fun getFieldOrder(): MutableList<String> = mutableListOf(
            "lStructSize", "hwndOwner", "hInstance", "lpstrFilter", "lpstrCustomFilter", "nMaxCustFilter",
            "nFilterIndex", "lpstrFile", "nMaxFile", "lpstrFileTitle", "nMaxFileTitle", "lpstrInitialDir",
            "lpstrTitle", "Flags", "nFileOffset", "nFileExtension", "lpstrDefExt", "lCustData", "lpfnHook",
            "lpTemplateName", "pvReserved", "dwReserved", "FlagsEx"
        )
    }


    @Test
    fun t5_可変長構造体() {
        // https://docs.microsoft.com/en-us/windows/win32/api/ipexport/ns-ipexport-ip_interface_info

        // TODO
    }
}
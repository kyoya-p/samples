import com.sun.jna.platform.win32.Advapi32
import com.sun.jna.platform.win32.WinReg
import org.junit.jupiter.api.Test

class T42_JNA_Windows_TODO {
    @Test
    fun test1() {
        // see: https://docs.microsoft.com/ja-jp/windows/win32/sysinfo/windows-system-information

        val advapi32 = Advapi32.INSTANCE!!
        advapi32.RegOpenKeyEx(
            WinReg.HKEY_CURRENT_USER,
            "",
            //TODO
        )

    }

}
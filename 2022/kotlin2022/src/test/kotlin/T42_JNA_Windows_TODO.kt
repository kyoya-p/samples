import com.sun.jna.LastErrorException
import com.sun.jna.Memory
import com.sun.jna.platform.win32.*
import com.sun.jna.platform.win32.WinNT.HRESULT
import com.sun.jna.platform.win32.WinNT.REG_DWORD
import com.sun.jna.ptr.IntByReference
import com.sun.jna.ptr.LongByReference
import com.sun.jna.ptr.PointerByReference
import org.junit.jupiter.api.Test
import kotlin.String

fun formatMessage(code: Int): String {
    val buffer = PointerByReference()
    if (0 == Kernel32.INSTANCE.FormatMessage(
            WinBase.FORMAT_MESSAGE_ALLOCATE_BUFFER
                    or WinBase.FORMAT_MESSAGE_FROM_SYSTEM
                    or WinBase.FORMAT_MESSAGE_IGNORE_INSERTS,
            null,
            code,
            0,  // TODO: MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT)
            buffer,
            0,
            null
        )
    ) {
        throw LastErrorException(Kernel32.INSTANCE.GetLastError())
    }
    val s: String = buffer.getValue().getWideString(0)
    Kernel32.INSTANCE.LocalFree(buffer.getValue())
    return s
}

class T42_JNA_Windows_TODO {
    @Test
    fun test1() {
        // see: https://docs.microsoft.com/ja-jp/windows/win32/sysinfo/windows-system-information
        val phKey = WinReg.HKEYByReference()
        val rc1 = Advapi32.INSTANCE.RegOpenKeyEx(
            WinReg.HKEY_CURRENT_USER,
            "Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings",
            0, /*Reserver*/
            WinNT.KEY_ALL_ACCESS,
            phKey,
        )
        if (rc1 != WinError.ERROR_SUCCESS) return
        val pType = IntByReference(REG_DWORD)
        val pRes = LongByReference()
        val rc2 = Advapi32.INSTANCE.RegQueryValueEx(
            phKey.value,
            "ProxyEnable",
            0,/*Reserved*/
            pType,
            pRes,
            IntByReference(),
        )
        println("pType: ${pType.value}")
        if (rc2 == WinError.ERROR_SUCCESS) {
            println("success: ${pRes.value}")
        } else {
            println("failed: ${formatMessage(rc2)}")
        }
        Advapi32.INSTANCE.RegCloseKey(phKey.value)
    }
}
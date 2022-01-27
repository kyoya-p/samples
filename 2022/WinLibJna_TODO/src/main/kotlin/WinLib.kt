import com.sun.jna.Native
import com.sun.jna.platform.win32.IPHlpAPI
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.W32APIOptions


interface IPHlpAPI_kk : IPHlpAPI {

    fun GetInterfaceInfo(lpBuffer: ByteArray?, lpnSize: IntByReference)

    companion object {
        val INSTANCE get() = Native.load("IPHlpAPI", IPHlpAPI_kk::class.java, W32APIOptions.DEFAULT_OPTIONS)
    }
}


fun main() {
    val ipHlpApi = IPHlpAPI_kk.INSTANCE

    val lpnBuff = IntByReference()
    ipHlpApi.GetInterfaceInfo(null, lpnBuff)
    println(lpnBuff.value)
}

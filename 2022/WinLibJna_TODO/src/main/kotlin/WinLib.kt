import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.platform.win32.IPHlpAPI
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.W32APIOptions


interface IPHlpAPI_kk : IPHlpAPI {

    fun GetInterfaceInfo(lpBuffer: ByteArray?, lpnSize: IntByReference)

    // https://translate.google.com/website?sl=en&tl=ja&prev=search&u=http://msdn.microsoft.com/en-us/library/aa365801(VS.85).aspx
    fun AddIPAddress(
        Address: IPAddr,
        IpMask: IPAddr,
        IfIndex: WinDef.DWORD,
        NTEContext: Pointer, // Pointer<WinDef.ULONG>,
        NTEInstance: Pointer, // Pointer<WinDef.ULONG>,
    )

    companion object {
        val INSTANCE get() = Native.load("IPHlpAPI", IPHlpAPI_kk::class.java, W32APIOptions.DEFAULT_OPTIONS)
    }

    class IP_ADAPTER_INDEX_MAP(
        val Index: WinDef.ULONG,
        val Name: List<WinDef.USHORT>,
    )

    class IP_INTERFACE_INFO(
        val NumAdapters: WinDef.LONG,
        val Adapter: IP_ADAPTER_INDEX_MAP,
    )
}


fun main() {
    val ipHlpApi = IPHlpAPI_kk.INSTANCE
    IPHlpAPI.AF_INET

    val lpnBuff = IntByReference()
    ipHlpApi.GetInterfaceInfo(null, lpnBuff)
    println(lpnBuff.value)

    ipHlpApi.GetInterfaceInfo()
}

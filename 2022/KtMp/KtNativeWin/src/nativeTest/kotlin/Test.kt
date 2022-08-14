import interop.func_in_params
import interop.func_out_params
import kotlinx.cinterop.*
import platform.windows.*
import sample.getModuleFileName
import sample.createService
import sample.deleteService
import sample.unionSample
import kotlin.test.Test

class test {
    @Test
    fun uniontest() {
        unionSample()
    }

    @Test
    fun getCommandLine() {
        val pCommand = GetCommandLineW()
        println(pCommand?.toKString())
    }

    @Test
    fun GetModuleFileName() {
        println(getModuleFileName())
    }

    @Test
    fun installServiceTest() {
        createService()
    }

    @Test
    fun deleteServiceTest() {
        deleteService()
    }

    @Test
    fun test_in_params() = memScoped {
        val dw: DWORD = 0x12345678.toUInt() // DWORD = UInt
        val c: CHAR = 'c'.code.toByte() // CHAR = Byte
        val wc: WCHAR = 'w'.code.toUShort() //WCHAR = UShort
        val lpCStr: String = "const string" //  @CCall.CString LPCSTR型の仮引数にはkotlin.String?型を渡す
        val lpCWStr: String = "const wide string" //  @CCall.WCString LPCWSTR型の仮引数にはkotlin.String?型を渡す

        func_in_params(
            dw = dw,
            c = c,
            wc = wc,
            lpCStr = lpCStr,
            lpCWStr = lpCWStr
        )
    }

    @Test
    fun test_out_params() = memScoped {
        val lpDw_Out: LPDWORD = alloc<DWORDVar>().ptr //  LPDWORD = CPointer<DWORDVar>
        val lpStr_Out: LPSTR = allocArray<CHARVar>(256) //  LPSTR = CPointer<CHARVar>
        val lpWStr_Out: LPWSTR = allocArray<WCHARVar>(256) // LPWSTR = CPointer<WCHARVar>

        func_out_params(
            lpDW_Out = lpDw_Out,
            lpStr_Out = lpStr_Out,
            lpWStr_Out = lpWStr_Out
        )

        //出力を参照
        val resDw: UInt = lpDw_Out.pointed.value
        val resStr: String = lpStr_Out.toKString()
        val resWStr: String = lpWStr_Out.toKString()
    }

    @Test
    fun reinterpretCastTest() = memScoped {
        val bufferSize = sizeOf<DWORDVar>() * 10
        val buffer = allocArray<ByteVar>(bufferSize)
        fun printBuffer() = println((0 until bufferSize).map { buffer[it] }.joinToString { it.toString(0x10) })

        val pDw = buffer.reinterpret<DWORDVar>()
        pDw.pointed.value = 0x01020304u
        printBuffer()
    }
}
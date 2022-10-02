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
            val bufferSize = sizeOf<DWORDVar>() * 10 // DWORD 10個分のメモリサイズ取得(単位:バイト)
            val buffer = allocArray<ByteVar>(bufferSize) // Byteの配列として確保
            fun printBuffer() = println((0 until bufferSize).map { buffer[it] }.joinToString { it.toString(0x10) }) // デバック用

            val pDw = buffer.reinterpret<DWORDVar>() // Byteの配列の先頭ポインタをDWORDのポインタにキャスト
            pDw.pointed.value = 0x01020304u // ポインタが指すDWORD型変数に書き込み
            pDw[2] = 0x02020202u //ポインタを配列として扱い3番目のDWORD要素に書き込み

            val v: DWORD = pDw.pointed.value // ポインタが指すDWORD変数の値を参照
            val v2: DWORD = pDw[2] // // ポインタを配列として扱い、3番目の要素を参照

            assert(v == 0x01020304u)
            assert(v2 == 0x02020202u)
            println("v:DWORD=0x${v.toString(0x10)}")
            println("v2:DWORD=0x${v2.toString(0x10)}")
            printBuffer()
        }


}
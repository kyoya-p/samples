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
}
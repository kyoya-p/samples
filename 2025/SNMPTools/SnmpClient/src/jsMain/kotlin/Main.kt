import okio.FileSystem
import okio.NodeJsFileSystem
import snmp.netsnmp.js.Session
import snmp.netsnmp.js.createSession

//suspend fun main() = appMain()
actual val fileSystem: FileSystem = NodeJsFileSystem

suspend fun main() {
    val session = createSession("192.168.11.5", "public")
    session.getNext(arrayOf("1.3.6"), { e, vb -> println(e) })
}

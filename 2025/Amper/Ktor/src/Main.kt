import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.serialization.kotlinx.xml.xml
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.impl.multiplatform.InputStream
import nl.adaptivity.xmlutil.serialization.XML
import nl.adaptivity.xmlutil.serialization.XmlElement


suspend fun main() {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) { xml(format = XML { xmlDeclMode = XmlDeclMode.Charset }) }
    }
    val url = "https://batspi.com/index.php?cmd=listselect&sel=&rowid=30457&pcnt2=1"

    val response = client.get(url).bodyAsText()
//    val response = client.get(url).body<HTML>()
    println(response)
}


sealed class XmlNode
class XmlTagStart(name: String) : XmlNode()
class XmlTagEnd(name: String) : XmlNode()


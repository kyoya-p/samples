import com.ctc.wstx.stax.WstxInputFactory
import java.io.InputStream
import javax.xml.stream.XMLStreamConstants.*

fun main(args: Array<String>) {
    println("Hello World!")

    // Try adding program arguments via Run/Debug configuration.
    // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
    println("Program arguments: ${args.joinToString()}")
}

@OptIn(ExperimentalStdlibApi::class)
fun xmlCheck(inputStream: InputStream) {

    val inputFactory = WstxInputFactory()
    val xmlReader = inputFactory.createXMLStreamReader(inputStream)
    while (xmlReader.hasNext()) {
        when (val type = xmlReader.next()) {
            START_ELEMENT -> println("START_ELEMENT ${xmlReader.name}")
            END_ELEMENT -> println("END_ELEMENT ${xmlReader.name}")
            CHARACTERS -> println("CHARACTERS ${xmlReader.text}")
            START_DOCUMENT -> println("START_DOCUMENT")
            END_DOCUMENT -> println("END_DOCUMENT")
            ATTRIBUTE -> println("ATTRIBUTE ${xmlReader.name} ${xmlReader.text}")
            else -> println(type)
        }
    }
}

enum class NodeType(val i: Int) {
    START_ELEMENT(1),
    END_ELEMENT(2),
    PROCESSING_INSTRUCTION(3),
    CHARACTERS(4),
    COMMENT(5),
    SPACE(6),
    START_DOCUMENT(7),
    END_DOCUMENT(8),
    ENTITY_REFERENCE(9),
    ATTRIBUTE(10),
    DTD(11),
    CDATA(12),
    NAMESPACE(13),
    NOTATION_DECLARATION(14),
    ENTITY_DECLARATI(15),
}

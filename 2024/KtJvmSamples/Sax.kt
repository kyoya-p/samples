import org.xml.sax.Attributes
import org.xml.sax.ext.DefaultHandler2
import java.io.*
import javax.xml.parsers.SAXParserFactory

fun main(args: Array<String>) {
    parseXML(File(args[0]).inputStream(), File(args[1]).outputStream())
}

fun parseXML(input: InputStream, output: OutputStream, op: (qn: String, a: Attributes) -> Unit = { _, _ -> }) {
    val writer = output.bufferedWriter()
    val handler = object : DefaultHandler2() {
        fun String.escape() = map { c ->
            when (c) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&apos;"
                else -> "$c"
            }
        }.joinToString("")

        operator fun Attributes.get(i: Int) = " ${getQName(i)}=\"${getValue(i).escape()}\""
        fun Attributes.atrs() = (0 until length).joinToString("") { this[it] }
        override fun startElement(u: String, l: String, q: String, a: Attributes) = writer.write("<${q}${a.atrs()}>")
        override fun endElement(uri: String, localName: String, qName: String) = writer.write("</$qName>")
        override fun characters(ch: CharArray, s: Int, e: Int) = writer.write(String(ch, s, e).escape())
        override fun startDocument() = writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
    }
    SAXParserFactory.newInstance().newSAXParser().parse(input, handler)
    writer.flush()
}


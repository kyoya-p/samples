import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.File
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

fun main() {
    val doc = readXml("./test/dc.xml")
    //val nodeList = selectByXpath(doc, "//*[contains(name(),'-id')]")
    val nodeList = selectByXpath(doc, "//id")
    nodeList.map {
        ancestors(it).map { it.nodeName }.joinToString("/")
    }.distinct().forEach {
        println(it)
    }
}

// XML読み取り
fun readXml(fileName: String): Document {
    val xmlFile = File(fileName)
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    val xmlInput = InputSource(StringReader(xmlFile.readText()))
    val doc = dBuilder.parse(xmlInput)
    return doc
}

// XPATHでノードを抽出
fun selectByXpath(doc: Document, xpath: String): List<Node> {
    val xpFactory = XPathFactory.newInstance()
    val xPath = xpFactory.newXPath()
    val elementNodeList = xPath.evaluate(xpath, doc, XPathConstants.NODESET) as NodeList
    val res = (0 until elementNodeList.length).map { elementNodeList.item(it)!! }
    return res
}

// 親ノードを全て列挙
fun ancestors(node: Node): List<Node> {
    return generateSequence(node) { it.parentNode }.toList().reversed()
}

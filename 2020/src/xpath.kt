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
    select2("./test/sb.xml", "//*[not(*)]").forEach { path, v -> println("$path $v") }

    // select("./test/sb.xml", "(//id|//*[contains(name(),'-id')])").forEach { println(it) }
    //  select("./test/dc.xml", "(//id|//*[contains(name(),'-id')])").forEach { println(it) }
}

fun select(file: String, path: String): List<String> = selectByXpath(readXml(file), path)
        .map {
            generateSequence(it) { it.parentNode }.toList().reversed().drop(1) //親ノードをたどりすべて列挙し
                    .map { it.nodeName }.joinToString("/") //ノード名を文字列として結合
        }.map {
            it.replace("(\\d+)".toRegex(), "*") // 無用な重複排除のため、"数字/"=>'*/"に置き換え
        }.distinct()
        .map { it.replace("/data/value", " []") } //末尾の"/data/value"を'[]'に置き換え
        .map { "Doc($file): $it" }

fun Node.absPath() = generateSequence(this) { it.parentNode }.toList().reversed().drop(1)
        .map { it.nodeName }.joinToString("/").replace("(\\d+)".toRegex(), "*")

fun select2(file: String, path: String) =
        selectByXpath(readXml(file), path).groupingBy { it.absPath() }.fold(listOf<String>()) { r, t ->
            r + t.textContent.take(6).replace("[\\r\\n]".toRegex(), "")
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


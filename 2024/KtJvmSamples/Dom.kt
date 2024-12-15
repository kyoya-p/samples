import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

fun File.xml() = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)!!

fun adjustXml(srcFile: File, tgFile: File = srcFile, op: Document.() -> Unit) {
    val doc = srcFile.xml().apply(op)
    TransformerFactory.newInstance().newTransformer().transform(DOMSource(doc), StreamResult(tgFile))
}

fun Document.selectElement(xpath: String) =
    XPathFactory.newInstance().newXPath().compile(xpath).evaluate(this, XPathConstants.NODE) as Element

fun Document.selectElements(xpath: String) = (XPathFactory.newInstance().newXPath().compile(xpath)
    .evaluate(this, XPathConstants.NODESET) as NodeList)
    .let { ns -> (0 until ns.length).mapNotNull { ns.item(it) as? Element } }

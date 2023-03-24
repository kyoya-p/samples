import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

fun main() {
    val csvFile = File("C:\\Users\\s117781\\Downloads\\query_data.csv") // CSVファイルのパス
    val lines = csvFile.readLines() // CSVファイルを行ごとに読み込む
    for (line in lines) {
        if (line.startsWith("<?xml")) { // XMLデータが始まる行ならば
            val xmlString = line // XMLデータを文字列として取得
            val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(xmlString.byteInputStream()) // XML文字列をドキュメントオブジェクトに変換
            val modelCode = doc.getElementsByTagName("ModelCode").item(0).textContent // ModelCode要素の値を取得
            val serialNumber = doc.getElementsByTagName("SerialNumber").item(0).textContent // SerialNumber要素の値を取得
            println("ModelCode: $modelCode") // ModelCode要素の値を表示
            println("SerialNumber: $serialNumber") // SerialNumber要素の値を表示
        }
    }
}
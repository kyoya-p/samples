package gifts

import io.ktor.client.*
import io.ktor.client.request.*
import org.dom4j.io.SAXReader
import java.io.StringReader
import java.nio.charset.StandardCharsets

suspend fun mfpifDataExportRequest(req: HttpAgentRequest) {
    HttpClient().use { client ->
        val requestBody = """
            <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
            xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
                <soap:Body>
                    <exportData xmlns="urn:schemas-sharp-jp:service:mfp-1-1">
                        <security>
                            <access-token>
                                <username>admin</username>
                                <password>admin</password>
                            </access-token>
                        </security>
                        <category>
                            <item>all</item>
                        </category>
                    </exportData>
                </soap:Body>
            </soap:Envelope>
            """.trimIndent()
        val r = client.post<String>("http://10.36.102.245:80/mfpif-service") {
            header("SOAPAction", "urn:schemas-sharp-jp:service:mfp-1-1#exportData")
            body = requestBody
        }

        val document = SAXReader().read(StringReader(r))
        val node = document.selectSingleNode("//*[local-name()='data-url']")

        val url = node.text!!
        println(node.text)

        val v = String(client.get<ByteArray>(url), StandardCharsets.UTF_8)
        println(v)
    }
}

/*
<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
 <soap:Body>
 <exportData xmlns="urn:schemas-sharp-jp:service:mfp-1-1">
 <category>
 <item>all</item>
</category>
 </exportData>
 </soap:Body>
</soap:Envelope>
*/

/*

<SOAP-ENV:Envelope
xmlns:SOAP-ENV="http://schemas.xmlsoap.org/soap/envelope/"
xmlns:xsd="http://www.w3.org/2001/XMLSchema"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
<SOAP-ENV:Body>
<exportDataResponse
xmlns="urn:schemas-sharp-jp:service:mfp-1-1"
xmlns:mt="urn:schemas-sharp-jp:mfp:type-1-1"
SOAP-ENV:encodingStyle="http://schemas.xmlsoap.org/soap/encoding/"
xmlns:SOAP-ENC="http://schemas.xmlsoap.org/soap/encoding/">
<result-code>ok</result-code>
<data-url>http://10.36.102.245:6080</data-url>
</exportDataResponse>
</SOAP-ENV:Body>
</SOAP-ENV:Envelope>

 */
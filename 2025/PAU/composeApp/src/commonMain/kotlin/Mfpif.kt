import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*

expect fun client(): HttpClient

suspend fun getJoblog(url: String): String {
//    val response = client().post(url) { setBody(getJobLog) }.body<XmlElement>()
    val response = client().get(url).bodyAsText()
//    println(response)
    return ("$response")
}

val getJobLog = """<soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/"
xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
xmlns:xsd="http://www.w3.org/2001/XMLSchema">
<soap:Body>
<getJobLog xmlns="urn:schemas-sharp-jp:service:mfp-1-1">
<joblog-id>1</joblog-id>
<max-results>2</max-results>
</getJobLog>
</soap:Body>
</soap:Envelope>
"""


expect fun getIpV4Host(): String
expect fun getIpV4SubnetWidth(): Int
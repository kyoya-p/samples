package soapClientTrial

import io.ktor.client.*
import io.ktor.client.request.*
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    HttpClient().use { client ->
        val request = """
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
            """.trimIndent()
        val r:String =client.get("http://10.36.102.245/mfpif-service")
        println(r)
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


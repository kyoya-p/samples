import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.github.kittinunf.fuel.httpPost
import org.jsoup.Jsoup
import java.net.Authenticator
import java.net.PasswordAuthentication


/*
(参考,OK) https://qiita.com/kaakaa_hoe/items/d4fb11a3af035a287972

Tokenの作成について
1. GCPコンソール　> IAM管理 > サービスアカウント 作成, 操作 > 鍵を作成
2. GCPコンソールにさっきダウンロードした .jsonファイルをアップロードし、
3. export GOOGLE_APPLICATION_CREDENTIALS=.....json
4. gcloud auth application-default print-access-token

*/
fun main() {
    // 認証Proxyを通すときは Authenticatorの登録コードが必須なのか...
    // 今後HTTPを使うコード全てに必ずこれを入れよう
    Authenticator.setDefault(object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            if (requestorType === RequestorType.PROXY) {
                val protocol = requestingProtocol.toLowerCase()
                val user = System.getProperty("$protocol.proxyUser")
                        ?: throw Exception("Authentication Error: Set property of '$protocol.proxyUser'")
                val password = System.getProperty("$protocol.proxyPassword")
                        ?: throw Exception("Authentication Error: Set property of '$protocol.proxyPassword'")

                return PasswordAuthentication(user, password.toCharArray())
            } else {
                throw Exception("This Authenticator supports only proxy authentication.")
            }
        }
    })

    //val r = Jsoup.connect("https://asahi.com").post()
    //println(r.html())
    val reqBody = """
    {
      "model": "projects/docker-hub-kk/locations/global/models/general/base",
      "sourceLanguageCode": "en",
      "targetLanguageCode": "ja",
      "contents": ["Come here!"]
    }
""".trimIndent()
    val headers = mapOf(
            "Authorization" to "Bearer ya29.c.Ko8Bxwd1DDHs6Zzn-mWm4fQ96ZP8JrzA-M8l6qu10DPbTVhn_o6MSwjBOsiqmAQQyFA8Qke4Q4NgjUbMV6YWodQliaEf1qQy-4pBuKBPG8wFPK5DPO_RZIpKxv6xY5lDrapKrbx4PC5RO34A6qIteQll4w7AK0MHVBB59M30LBBUgFkRl9mQKmYfrKZshtz7lFE"
            , "Content-Type" to "application/json; charset=utf-8"
    )

    val r = "https://translation.googleapis.com/v3beta1/projects/docker-hub-kk/locations/global:translateText".httpPost().header(headers).body(reqBody).response().second
    println(r.data.toString(Charsets.UTF_8))
}



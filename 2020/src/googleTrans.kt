import org.jsoup.Jsoup

/* Memo:
プロキシ設定 for Java
set JAVA_TOOL_OPTIONS=-Djdk.http.auth.tunneling.disabledSchemes="" -Dhttps.proxyHost=172.29.241.32 -Dhttps.proxyPort=807 -Dhttps.proxyUser=admin -Dhttps.proxyPassword=admin -Dhttp.proxyHost=172.29.241.32 -Dhttp.proxyPort=807 -Dhttp.proxyUser=admin -Dhttp.proxyPassword=admin

-Djdk.http.auth.tunneling.disabledSchemes=""
-Dhttps.proxyHost=172.29.241.32
-Dhttps.proxyPort=807
-Dhttps.proxyUser=admin
-Dhttps.proxyPassword=admin
-Dhttp.proxyHost=172.29.241.32
-Dhttp.proxyPort=807
-Dhttp.proxyUser=admin
-Dhttp.proxyPassword=admin

-Djdk.http.auth.tunneling.disabledSchemes=""
-Dhttps.proxyHost=10.144.98.31
-Dhttps.proxyPort=3080
-Dhttps.proxyUser=
-Dhttps.proxyPassword=




*/
fun main() {
//    settingOfJavaProxy()

    val googleTranslate = "https://translate.google.com/?op=translate&sl=ja&tl=en&text=こんにちは"

    val res = Jsoup.connect(googleTranslate).get()
    println(res.toString())

}

fun settingOfJavaProxy() {
    System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "")
}
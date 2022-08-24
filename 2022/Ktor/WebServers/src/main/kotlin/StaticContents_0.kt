import io.ktor.http.content.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File

//    implementation("io.ktor:ktor-server-core:1.6.7")
//    implementation("io.ktor:ktor-server-netty:1.6.7")

@Suppress("JSON_FORMAT_REDUNDANT")
fun main() {
    embeddedServer(Netty, port = 8080) {
        routing {
            static("static") {
                // コンテンツのルートフォルダのデフォルトはカレント実行ディレクトリ(プロジェクトルート)
                staticRootFolder = File(System.getProperty("user.dir")) // デフォルト以外を指定する場合
                files("css")
                files("js")
                file("image.png")
                file("random.txt", "image.png")
                default("index.html")
            }
        }
    }.start(wait = true)
}

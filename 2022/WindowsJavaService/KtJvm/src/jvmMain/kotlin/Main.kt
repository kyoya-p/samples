import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.html.*
import java.io.File


val testPort = 18080
fun server() = embeddedServer(CIO, port = testPort) {
    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = "/off"
        exitCodeSupplier = { 0 }
    }
    routing {
        get("/") { call.respondHtml(HttpStatusCode.OK) { myForm() } }
        get("/run") { call.respondText("Hello ${call.parameters["name"]}.") }
        get("/upd") {
            val msiFileName = "${call.parameters["msi"]}"
            val log = runMsi(msiFileName)
            call.respondText("running $msiFileName.\n$log")
            delay(15_000)
        }
    }
}

fun HTML.myForm() = body {
    form(method = FormMethod.get) {
        textArea { rows = "5"; name = "cmds";style = "width:100%;"; text("c:\\Windows\\System32\\msiexec.exe\n/i\nc:\\temp\\\n/qn") }
        br
        input(type = InputType.submit) { value = "実行" }
    }
    label { text("c:\\Windows\\System32\\msiexec.exe") }
}

fun runMsi(msiFileName: String): String {
//    val pb = ProcessBuilder("C:\\Windows\\System32\\msiexec.exe", "/i", "c:\\temp\\$msiFileName", "/qn")
    val pb = ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", "mkdir", "c:\\temp\\$msiFileName")
    val log = File("process.txt")
    pb.redirectOutput(log)
    pb.redirectErrorStream(true)
    pb.start()
    println("start '${pb.command()}'.")
    val r = log.readText()
    println("log '$r'.")
    return r
}

val sem = Semaphore(permits = 1, acquiredPermits = 1)
fun main(): Unit = runBlocking {
    println("startup service.")
    server().start()
    sem.acquire()
    println("term.")
}

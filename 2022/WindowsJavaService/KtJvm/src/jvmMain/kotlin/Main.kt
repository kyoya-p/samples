import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.datetime.Clock.System.now
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

val testPort = 18080
fun server() = embeddedServer(CIO, port = testPort) {
    install(ShutDownUrl.ApplicationCallPlugin) {
        shutDownUrl = "/off"
        exitCodeSupplier = {
            println("shutdown.")
            0
        }
    }
    routing {
        get("/") {
            call.respondText("Hello ${call.parameters["name"]}.")
            println("Hello ${call.parameters["name"]}.")
        }
        get("/upd") {
            val msiFileName = "${call.parameters["msi"]}"
            call.respondText("running $msiFileName.")
            runMsi(msiFileName)
            delay(15_000)
            sem.release()
        }
    }
}

fun runMsi(msiFileName: String) {
//    val pb = ProcessBuilder("C:\\Windows\\System32\\msiexec.exe", "/i", "c:\\temp\\$msiFileName", "/qn")
    val pb = ProcessBuilder("C:\\Windows\\System32\\cmd.exe", "/c", "mkdir", "c:\\temp\\$msiFileName")
    pb.start()
    println("start '${pb.command()}'.")
}

val sem = Semaphore(permits = 1, acquiredPermits = 1)
fun main(): Unit = runBlocking {
    println("startup service.")
    server().start()
    sem.acquire()
    println("term.")
}

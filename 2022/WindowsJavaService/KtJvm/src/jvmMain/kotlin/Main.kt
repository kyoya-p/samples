import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore

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
            println("running $msiFileName .")
            runMsi(msiFileName)
            sem.release()
        }
    }
}

fun runMsi(msiFileName: String) {
    val pb = ProcessBuilder("msiexec.exe", "/i", "c:/temp/$msiFileName", "/qn")
    pb.start()
}

val sem = Semaphore(permits = 1, acquiredPermits = 1)
fun main(): Unit = runBlocking {
    println("startup service.")
    server().start()
    sem.acquire()
    println("term.")
}

import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking

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
        }
    }
}

fun runMsi(msiFileName: String) {
    ProcessBuilder("msiexec.exe", "/i", "c:/temp/$msiFileName","/qn").start()
}

fun main(): Unit  {
    println("startup service.")
    server().start(wait = true)
    println("shutdown server") // Shutdownはserver内でプロセス終了? ここは通過しない
}

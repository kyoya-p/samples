import io.ktor.application.*

fun main(args: Array<String>) = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    install() {
    }
}

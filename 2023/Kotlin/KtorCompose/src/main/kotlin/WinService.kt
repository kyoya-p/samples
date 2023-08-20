import io.ktor.server.engine.*
import org.apache.commons.daemon.Daemon
import org.apache.commons.daemon.DaemonContext

fun main(args: Array<String>) = when (args.getOrNull(0)) {
    "start" -> {}
    "stop" -> {}
    else -> {}
}

@Suppress("unused")
class KtorDaemon : Daemon {
    var service: ApplicationEngine? = null

    @Throws(Exception::class)
    override fun init(context: DaemonContext) {
    }

    @Throws(Exception::class)
    override fun start() {
        service = appServer()
    }

    @Throws(Exception::class)
    override fun stop() {
        service?.stop()
    }

    @Throws(Exception::class)
    override fun destroy() {
    }
}

fun register() {
    val pb = ProcessBuilder("java", "-version")
    val process = pb.start()
}
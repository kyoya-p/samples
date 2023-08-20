import org.apache.commons.daemon.Daemon
import org.apache.commons.daemon.DaemonContext

@Suppress("unused")
class KtorDaemon : Daemon {
    @Throws(Exception::class)
    override fun init(context: DaemonContext) {
        println("Hello, world!")
    }

    @Throws(Exception::class)
    override fun start() {
        main()
    }

    @Throws(Exception::class)
    override fun stop() {
        // do something...
    }

    @Throws(Exception::class)
    override fun destroy() {
        // do something...
    }
}

class Service : Runnable {
    override fun run() {
        TODO("Not yet implemented")
    }
}

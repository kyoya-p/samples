import org.apache.commons.daemon.Daemon
import org.apache.commons.daemon.DaemonContext


class HelloWorldDaemon : Daemon {
    @Throws(Exception::class)
    override fun init(context: DaemonContext) {
        println("Hello, world!")
    }

    @Throws(Exception::class)
    override fun start() {
        // do something...
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

class Service: Runnable{
    override fun run() {
        TODO("Not yet implemented")
    }
}
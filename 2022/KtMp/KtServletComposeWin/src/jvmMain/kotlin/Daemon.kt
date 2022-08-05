package com.sample.service

import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class HelloWorldServiceLauncher {
    private var executor: ExecutorService? = null
    fun initialize() {
        if (service == null) {
            service = service(8080)
        }
        executor = Executors.newSingleThreadExecutor()
        executor!!.execute(service)
    }

    fun terminate() {
        if (service != null) {
            service.stop()
        }
        if (executor != null) {
            executor!!.shutdown()
        }
    }

    companion object {
//        private val LOG: org.apache.commons.logging.Log = org.apache.commons.logging.LogFactory.getLog(
//            HelloWorldServiceLauncher::class.java
//        )
        private var service: IService? = null
        private val instance = HelloWorldServiceLauncher()
        private var scanner: Scanner? = null
        @JvmStatic
        fun main(args: Array<String>) {
            if (args != null) {
//                LOG.debug("Param : " + Arrays.toString(args))
            }
            start(null)
            scanner = Scanner(System.`in`)
//            LOG.debug("Enter 'stop' to halt: ")
            while (scanner!!.nextLine().lowercase(Locale.getDefault()) != "stop") {
            }
            stop(null)
        }

        fun start(args: Array<String?>?) {
            if (args != null) {
//                LOG.debug("Param : " + Arrays.toString(args))
            }
            instance.initialize()
        }

        fun stop(args: Array<String?>?) {
            if (args != null) {
//                LOG.debug("Param : " + Arrays.toString(args))
            }
            instance.terminate()
        }
    }
}
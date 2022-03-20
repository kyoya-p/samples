package testenv

import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import kotlin.concurrent.thread
import kotlin.time.ExperimentalTime

class TestEnv {
    inner class Pipe(val pipeName: String) {
        val intake = object : PipedOutputStream() {
            override fun write(b: ByteArray) {
                if (debug) rawSo.print("$pipeName $b")
                super.write(b)
            }

            override fun write(b: Int) {
                if (debug) rawSo.print("$pipeName $b")
                super.write(b)
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                if (debug) rawSo.print("$pipeName ${String(b, off, len)}")
                super.write(b, off, len)
            }
        }
        val outlet = PipedInputStream(intake)
    }

    var debug: Boolean = false
    val rawSi = System.`in`
    val rawSo = System.out
    val si = Pipe("SI>>")
    val so = Pipe("SO<<")

    val intake = PrintStream(si.intake)
    val outlet = so.outlet.bufferedReader()

    @Suppress("unused")
    fun <T> println(v: T) = if (debug) rawSo.println(v) else Unit

    @Suppress("unused")
    fun <T> print(v: T) = if (debug) rawSo.print(v) else Unit
}

fun <TR> stdioEmulatior(envSvr: TestEnv.() -> Boolean, target: () -> TR): Boolean {
    val env = TestEnv()
    System.setIn(env.si.outlet)
    System.setOut(PrintStream(env.so.intake))
    var thRes: Boolean? = null
    val thEnv = thread {
        runCatching {
            //env.rawSo.println("Test Environment Start.")
            thRes = env.envSvr()
            //env.rawSo.println("Test Environment Complete.")
        }.onFailure { it.printStackTrace(System.err) }.getOrThrow()
    }

    //env.rawSo.println("Target Start.")
    //val start = Clock.System.now().toEpochMilliseconds()
    target()
    //val lap = Clock.System.now().toEpochMilliseconds() - start
    thEnv.join()
    System.setIn(env.rawSi)
    System.setOut(env.rawSo)
    return thRes!!
}

fun <TR> stdioEmulatior(envSvrs: List<TestEnv.() -> Boolean>, target: () -> TR) =
    envSvrs.asSequence().mapIndexed { i, it ->
        System.err.println("Test ${i + 1}: -----------")
        val rc = stdioEmulatior(it, target)
        System.err.println("Test ${i + 1}: $rc")
    }.toList()

fun <R> testEnv(e: TestEnv.() -> R) = e


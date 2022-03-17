package testenv

import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import kotlin.concurrent.thread

class TestEnv {
    inner class Pipe(val pipeName: String) {
        val intake = object : PipedOutputStream() {
            override fun write(b: ByteArray) {
                rawSo.print("$pipeName $b")
                super.write(b)
            }

            override fun write(b: Int) {
                rawSo.print("$pipeName $b")
                super.write(b)
            }

            override fun write(b: ByteArray, off: Int, len: Int) {
                rawSo.print("$pipeName ${String(b, off, len)}")
                super.write(b, off, len)
            }
        }
        val outlet = PipedInputStream(intake)
    }

    val rawSi = System.`in`
    val rawSo = System.out
    val si = Pipe("SI>>")
    val so = Pipe("SO<<")

    val intake = PrintStream(si.intake)
    val outlet = so.outlet.bufferedReader()

    @Suppress("unused")
    fun <T> println(v: T) = rawSo.println(v)

    @Suppress("unused")
    fun <T> print(v: T) = rawSo.print(v)
}

fun <ER, TR> stdioEmulatior(envSvr: TestEnv.() -> ER, target: () -> TR): ER {
    val env = TestEnv()
    System.setIn(env.si.outlet)
    System.setOut(PrintStream(env.so.intake))
    var thRes: ER? = null
    val thEnv = thread {
        runCatching {
            env.rawSo.println("Test Environment Start.")
            thRes = env.envSvr()
            env.rawSo.println("Test Environment Complete.")
        }.onFailure { it.printStackTrace(System.err) }.getOrThrow()
    }

    env.rawSo.println("Target Start.")
    target()
    env.rawSo.println("Target Complete.")

    thEnv.join()
    System.setIn(env.rawSi)
    System.setOut(env.rawSo)

    System.err.println(thRes)
    return thRes!!
}

fun <ER, TR> stdioEmulatior(envSvrs: List<TestEnv.() -> ER>, target: () -> TR) =
    envSvrs.asSequence().mapIndexed { i, it ->
        System.err.print("Test ${i+1}: ")
        stdioEmulatior(it, target)
    }.toList()

fun testEnv(e: TestEnv.() -> Unit) = e


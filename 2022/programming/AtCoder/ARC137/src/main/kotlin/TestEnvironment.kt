package testenv

import java.io.File
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.io.PrintStream
import kotlin.concurrent.thread

class TestEnv(val debug: Boolean = false) {
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

fun <TR> stdioEmulatior(debug: Boolean = false, envSvr: TestEnv.() -> Boolean, target: () -> TR): Boolean {
    val env = TestEnv(debug)
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
    envSvrs.asSequence().withIndex().all { (i, it) ->
        stdioEmulatior(true, it, target)
            .also { rc -> System.err.println("Test[${i + 1}] $: $rc -----------") }
    }.also { rc -> System.err.println("All Test: $rc -----------") }

fun <R> testEnv(e: TestEnv.() -> R): TestEnv.() -> R = e


fun testEnv_AtCoder(testDir: String, testName: String, debug: Boolean = false): List<TestEnv.() -> Boolean> =
    File("$testDir/$testName/in").listFiles()!!.map { inFile ->
        val outFile = File("$testDir/$testName/out/${inFile.name}")
        testEnv {
            intake.println(inFile.readText())
            val tg = outFile.bufferedReader().readLine()!!
            rawSo.println("exp:$tg")
            outlet.readLine() == tg
        }
    }


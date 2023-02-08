import java.io.*
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

    val rawSi = System.`in`!!
    val rawSo = System.out!!
    val si = Pipe("SI>>")
    val so = Pipe("SO<<")

    val intake = PrintStream(si.intake)
    val outlet = so.outlet.bufferedReader()

    fun PrintStream.put(s: String) = println(s)
    fun BufferedReader.observe(s: String) =
        s.trim().split("\n").all { t ->
            val r = readLine()
            (r == t).also { System.err.println("observed:'$t'='$r':$it") }
        }

    @Suppress("unused")
    fun <T> println(v: T) = if (debug) rawSo.println(v) else Unit

    @Suppress("unused")
    fun <T> print(v: T) = if (debug) rawSo.print(v) else Unit
}

fun <TR> stdioEmulator(debug: Boolean = false, envSvr: TestEnv.() -> Boolean, target: () -> TR): Boolean {
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

fun <TR> stdioEmulators(envSvs: List<TestEnv.() -> Boolean>, target: () -> TR) {
    envSvs.asSequence().withIndex().all { (i, it) ->
        stdioEmulator(true, it, target)
            .also { rc -> System.err.println("Test[$i] $rc -----------") }
    }.also { rc -> System.err.println("All Test: $rc -----------") }
}

fun testEnvBuilder(e: TestEnv.() -> Boolean): TestEnv.() -> Boolean = e


@Suppress("unused")
fun testEnv_AtCoder(
    testDir: String,
    testName: String,
    @Suppress("UNUSED_PARAMETER") debug: Boolean = false
): List<TestEnv.() -> Boolean> = File("$testDir/$testName/in").listFiles()!!.map { inFile ->
    val outFile = File("$testDir/$testName/out/${inFile.name}")
    testEnvBuilder {
        intake.println(inFile.readText())
        val tg = outFile.bufferedReader().readLine()!!
        rawSo.println("test:${inFile.name} - exp:$tg")
        outlet.readLine() == tg
    }
}

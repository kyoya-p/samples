import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock.System.now
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.util.*
import kotlin.NoSuchElementException
import kotlin.reflect.KProperty
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime

@Suppress("NonAsciiCharacters", "ClassName")
class `T4-Delegate` {
    @Test
    fun `t01-lazy遅延初期化`() {
        var c = 3

        val d by lazy { c } // immutableな変数

        println(d)
        assert(d == 3)
        c = 5
        println(c)
        assert(d == 3)
    }

    @Test
    fun `t02-デリゲートの定義_プロパティデリゲート`() {
        class month(private var v: Int = 1) {
            operator fun getValue(thisRef: Any?, property: KProperty<*>) = v
            operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) {
                v = (value - 1).mod(12) + 1
            }
        }

        var a by month()
        a = 12
        assert(a == 12)
        a += 1
        @Suppress("KotlinConstantConditions")
        assert(a == 1)
        a -= 2
        println(a)
        assert(a == 11)
    }

    @ExperimentalTime
    @Test
    fun `t03-デリゲートの定義_クラスデリゲート`(): Unit = runBlocking {
        class ThrottledQueue<E>(
            val inner: Channel<E> = Channel(Channel.RENDEZVOUS),
        ) : Channel<E> by inner {
            val n by lazy { now() }
            override suspend fun receive(): E {
                delayUntilNextPeriod(30.milliseconds, start = n)
                return inner.receive()
            }
        }

        val que = ThrottledQueue<Int>()
        stopWatch { w ->
            launch {
                runCatching {
                    while (isActive) {
                        val r = que.receive()
                        println("${w.now()} $r")
                    }
                }
            }

            for (i in 1..5) {
                que.send(i)
            }
            que.close()
        }
    }

    @Suppress("TestFunctionName")
    @Test
    fun `t04-by Map`() {
        val m = mutableMapOf("a" to "b")
        var a: String by m
        println(a)
        assert(a == "b")

        @Suppress("UNUSED_VALUE")
        a = "z"
        println(m)
        assert(m["a"] == "z")

        var b: String by m
        assertThrows<NoSuchElementException> {
            println(b)
        }
        b = "new"
        println(m)
        assert(m["b"] == "new")

        // JavaのDictionaryもOK
        val props = Properties()

        @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")
        var ja: String by props
        @Suppress("UNUSED_VALUE")
        ja = "value of ja"
        println(props)
        assert(props["ja"] == "value of ja")
    }

    @Suppress("TestFunctionName")
    @Test
    fun t05_PropertiesFile_変数名を参照() {
        class MyFileProperty(val actualPropName: String? = null) {
            val propFile = File("build/my.properties")
            operator fun getValue(thisRef: String?, property: KProperty<*>): String? {
                val prop = Properties()
                if (propFile.exists()) prop.load(propFile.inputStream())
                return prop.getProperty(actualPropName ?: property.name)
            }

            operator fun setValue(thisRef: String?, property: KProperty<*>, value: String?) {
                val prop = Properties()
                if (propFile.exists()) prop.load(propFile.inputStream())
                prop.setProperty(actualPropName ?: property.name, value)
                prop.store(propFile.outputStream(), "Comment for propertied file")
            }
        }

        run {
            @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "UNUSED_VARIABLE")
            var userName by MyFileProperty()

            @Suppress("ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE", "UNUSED_VARIABLE")
            var count by MyFileProperty()

            @Suppress("UNUSED_VALUE")
            userName = "root"
            @Suppress("UNUSED_VALUE")
            count = 100.toString()
        }

        println(File("build/my.properties").readText())

        run {
            val userName by MyFileProperty()
            val count by MyFileProperty()

            assert(userName == "root")
            assert(count == "100")
        }
    }
}


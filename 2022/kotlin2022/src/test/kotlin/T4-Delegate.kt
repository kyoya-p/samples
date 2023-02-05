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

    @Suppress("KotlinConstantConditions")
    @Test
    fun `t02-移譲_プロパティ移譲`() {
        class Max<T : Comparable<T>>(private var v: T) { // 現在値より小さい値は格納されない
            operator fun getValue(r: Any?, property: KProperty<*>) = v
            operator fun setValue(r: Any?, property: KProperty<*>, value: T) = apply { if (v < value) v = value }
        }

        var a by Max(0)
        a = 99; assert(a == 99)
        a = 100; assert(a == 100)
        a = 99; assert(a == 100) // IntelliJは「常にa==99」と言って警告するが..
    }

    @ExperimentalTime
    @Test
    fun `t03-移譲_クラス移譲`(): Unit = runBlocking {
        class ThrottledQueue<E>(
            val inner: Channel<E> = Channel(Channel.RENDEZVOUS),
        ) : Channel<E> by inner {
            val n by lazy { now() }
            override suspend fun receive(): E { // 帯域制限付きreceive。投入(send)を制限すべきか..
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

    @Test
    fun `t03b-移譲_1オリジンの配列`() {
        class ListWithOrigin<E>(
            val inner: List<E>,
            val origin: Int = 1
        ) : List<E> by inner {
            override fun get(index: Int) = inner.get(index - origin)
            override fun listIterator(index: Int) = inner.listIterator(index - origin)
            override fun subList(fromIndex: Int, toIndex: Int) = inner.subList(fromIndex - origin, toIndex - origin)
        }

        val v = ListWithOrigin(listOf(1, 2, 3))
        assert(v[1] == 1)

    }

    @Test
    fun `t04-by Map`() {
        val m = mutableMapOf("a" to "b")
        var a: String by m
        println(a)
        assert(a == "b")

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

        var ja: String by props
        ja = "value of ja"
        println(props)
        assert(props["ja"] == "value of ja")
    }

    @Test
    fun t05_PropertiesFile() {
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
            var userName by MyFileProperty()
            var count by MyFileProperty()
            userName = "root"
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


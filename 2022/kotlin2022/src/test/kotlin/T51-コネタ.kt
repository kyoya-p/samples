import org.junit.jupiter.api.Test
import java.net.InetAddress


@Suppress("ClassName", "NonAsciiCharacters", "TestFunctionName")
class `T51-コネタ` {
    @Test
    fun 式の途中にデバッグ表示を挟む() {
        val res = "hello [shokkaa]!!"
            .dropWhile { it != '[' }.drop(1)
            .takeWhile { it != ']' }
            .also(::println) // <=
            .let { "<$it>" }
        println(res)
    }

    @Test
    fun localhostを取得() {
        val adr = InetAddress.getByName("localhost")
        println("adr.canonicalHostName(): ${adr.canonicalHostName}")
        println("adr.hostName(): ${adr.hostName}")
        println("adr.hostAddress(): ${adr.hostAddress}")
    }

    @Test
    fun ローカルホスト名を取得() {
        val adr = InetAddress.getLocalHost()!!
        println("adr.canonicalHostName(): ${adr.canonicalHostName}")
        println("adr.hostName(): ${adr.hostName}")
        println("adr.hostAddress(): ${adr.hostAddress}")
    }

    @Test
    fun ローカルホスト名に対応するIPアドレスすべてを取得() {
        val hostAdr = InetAddress.getLocalHost()!!
        val adrs = InetAddress.getAllByName(hostAdr.hostName)
        adrs.forEachIndexed { i, adr ->
            println("$i: adr.hostAddress(): ${adr.hostAddress}")
        }
    }

}

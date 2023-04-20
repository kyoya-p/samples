import org.junit.jupiter.api.Test
import java.io.StringBufferInputStream

class MainTest {
    @Test
    fun test1() {
        val xml = """<a><b>BBB</b><c a1="a1"/></a>"""
        xmlCheck(StringBufferInputStream(xml))
    }
}
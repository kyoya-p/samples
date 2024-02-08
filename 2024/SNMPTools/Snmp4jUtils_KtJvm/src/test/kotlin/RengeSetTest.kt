import jp.wjg.shokkaa.snmp4jutils.IntRangeSet
import org.junit.jupiter.api.Test


class RengeSetTest {
    @Test
    fun addTest() {
        val r1 = IntRangeSet(listOf(4..7))
        println(r1.add(1..3))
    }
}

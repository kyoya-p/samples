import jp.wjg.shokkaa.snmp4jutils.IntRangeSet
import jp.wjg.shokkaa.snmp4jutils.totalLength
import org.junit.jupiter.api.Test


class RengeSetTest {
    @Test
    fun construction1() {
        val r1 = IntRangeSet() // empty
        assert(r1.size == 0)
    }

    @Test
    fun construction2() {
        val r1 = IntRangeSet(listOf())
        assert(r1.size == 0)
    }

    @Test
    fun construction3_order() {
        val r1 = 1..2
        val r2 = 5..6

        val rs1 = IntRangeSet(listOf(r1, r2))
        val rs2 = IntRangeSet(listOf(r2, r1))

        assert(rs1 == rs2)
        assert(rs1.toList()[0] == r1)
        assert(rs1.toList()[1] == r2)
        assert(rs2.toList()[0] == r1)
        assert(rs2.toList()[1] == r2)
    }

    @Test
    fun equal1() {
        val r1 = IntRangeSet(listOf(4..7))
        val r2 = IntRangeSet(listOf(4..7))
        assert(r1 == r2)
    }

    @Test
    fun add1() {
        val rs = IntRangeSet()
        val rx = 7..4
        val r = 4..7
        assert(rs.add(rx) == false)
        assert(rs.size == 0)

        assert(rs.add(r) == true)
        assert(rs.size == 1)

        assert(rs.add(r) == false)
        assert(rs.size == 1)

        assert(rs.add(rx) == false)
        assert(rs.size == 1)
    }

    @Test
    fun add2() {
        val rs = IntRangeSet()
        val r1 = 1..1
        val r2 = 2..2
        val r3 = 3..3
        rs.add(r1)
        assert(rs.size == 1)
        rs.add(r3)
        assert(rs.size == 2)
        rs.add(r2)
        assert(rs.size == 1)
        assert(rs.toList()[0].start == 1)
        assert(rs.toList()[0].endInclusive == 3)
    }

    @Test
    fun totalLength1() {
        val rs = IntRangeSet()
        val r1 = 1..10
        rs.add(r1)
        assert(rs.totalLength() == 10)
        rs.add(21..30)
        assert(rs.totalLength() == 20)
    }
}

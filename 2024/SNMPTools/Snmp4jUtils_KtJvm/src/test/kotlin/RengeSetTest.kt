import jp.wjg.shokkaa.snmp4jutils.IntRangeSet
import jp.wjg.shokkaa.snmp4jutils.ULongRangeSet
import jp.wjg.shokkaa.snmp4jutils.totalLength
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock.System.now
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds


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

        @Suppress("EmptyRange")
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
        rs.add(1..10)
        assert(rs.totalLength() == 10)
        rs.add(21..30)
        assert(rs.totalLength() == 20)
    }

    @Test
    fun totalLength_ULong() {
        val rs = ULongRangeSet()
        rs.add(1UL..10UL)
        assert(rs.totalLength() == 10UL)
        rs.add(21UL..30UL)
        assert(rs.totalLength() == 20UL)
    }

    @Test
    fun remove_ULong() {
        val rs = ULongRangeSet()
        rs.add(1UL..10UL)
        assert(rs.toList()[0] == 1UL..10UL)
        rs.remove(3UL..5UL)
        assert(rs.size == 2)
        assert(rs.toList()[0] == 1UL..2UL)
        assert(rs.toList()[1] == 6UL..10UL)
    }

    @Test
    fun removeAll_ULong() {
        val rs0 = ULongRangeSet(1UL..1000UL)
        val rs1 = rs0
        val rs2 = ULongRangeSet(41UL..42UL, 51UL..52UL, 998UL..1000UL)
        rs1.removeAll(rs2)
        assert(rs1.size == 3)
        assert(rs1.toList()[0] == 1UL..40UL)
        assert(rs1.toList()[1] == 43UL..50UL)
        assert(rs1.toList()[2] == 53UL..997UL)

        rs1.addAll(rs2)
        assert(rs1.size == 1)
        assert(rs1 == rs0)
    }

    @Test
    fun sequence1() {
        val rs = IntRangeSet(1..1, 3..5, 7..7)
        assert(rs.toList() == listOf(1..1, 3..5, 7..7))
    }

    @Test
    fun sequence2() {
        val Ki = 1024
        val Gi = Ki * Ki * Ki
        var i = 0
        runCatching {
            val rs = mutableListOf<Int>()

            (0..20767724).forEach {
                rs.add(-i)
                i++
            }
            println("size: ${rs.size}")
            rs.sort()
            rs.take(100).also(::println)
            rs.takeLast(100).also(::println)
            mutableListOf(1, 2, 3).sort()
        }.onFailure {
            println("Error: Last index: $i, ${i / Ki / Ki}M")
            it.printStackTrace()
        }

        class X4<T>(val i0: T, val i1: T = i0, val i2: T = i0, val i3: T = i0)
    }

    @Test
    fun flowTest() = runBlocking {
        repeat(5) {
            println(now())
            delay(1000.milliseconds)
        }
    }
}
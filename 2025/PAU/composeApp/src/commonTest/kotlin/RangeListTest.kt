import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import jp.wjg.shokkaa.snmp.RangeSet
import jp.wjg.shokkaa.snmp.asFlatSequence
import jp.wjg.shokkaa.snmp.toRangeList
import kotlin.time.ExperimentalTime


@OptIn(ExperimentalTime::class, ExperimentalStdlibApi::class)
class OpenEndRangeListTest : FunSpec({
    test("rangeList") {
        val r1 = 1UL..1UL
        val r2 = 2UL..2UL
        val r3 = 3UL..3UL

        listOf<ClosedRange<ULong>>().toRangeList() shouldBe listOf()
        listOf(r1).toRangeList() shouldBe listOf(1UL..1UL)
        listOf(r1, r2).toRangeList() shouldBe listOf(1UL..2UL)
        listOf(r2, r1).toRangeList() shouldBe listOf(1UL..2UL)
        listOf(r1, r3).toRangeList() shouldBe listOf(1UL..1UL, 3UL..3UL)
        listOf(r1, r3, r2).toRangeList() shouldBe listOf(1UL..3UL)

        val r13: ClosedRange<ULong> = 1UL..3UL
        val r27: ClosedRange<ULong> = 2UL..7UL
        val r59: ClosedRange<ULong> = 5UL..9UL
        val r18: ClosedRange<ULong> = 1UL..8UL

        listOf(r13, r59).toRangeList() shouldBe listOf(1UL..3UL, 5UL..9UL)
        listOf(r27, r18).toRangeList() shouldBe listOf(1UL..8UL)
        listOf(r18, r27).toRangeList() shouldBe listOf(1UL..8UL)
        listOf(r13, r59, r27).toRangeList() shouldBe listOf(1UL..9UL)
        listOf(r59, r13, r27).toRangeList() shouldBe listOf(1UL..9UL)
        listOf(r27, r59, r13).toRangeList() shouldBe listOf(1UL..9UL)

        val r19 = 1UL..9UL
        val r01 = 0UL..1UL

        fun List<ClosedRange<ULong>>.asFlatSequence() = asFlatSequence { it + 1UL }

        listOf<ULongRange>().toRangeList().asFlatSequence { it + 1UL }.toList() shouldBe listOf()
        listOf(r1).toRangeList().asFlatSequence().toList() shouldBe listOf(1UL)
        listOf(r1, r2).toRangeList().asFlatSequence().toList() shouldBe listOf(1UL, 2UL)
        listOf(r2, r1).toRangeList().asFlatSequence().toList() shouldBe listOf(1UL, 2UL)
        listOf(r1, r3).toRangeList().asFlatSequence().toList() shouldBe listOf(1UL, 3UL)
    }
})


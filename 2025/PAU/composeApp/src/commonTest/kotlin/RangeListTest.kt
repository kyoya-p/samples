import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import jp.wjg.shokkaa.snmp.asFlatSequence
import jp.wjg.shokkaa.snmp.toIpV4RangeSet
import jp.wjg.shokkaa.snmp.toIpV4ULong
import jp.wjg.shokkaa.snmp.toRangeList
import java.net.UnknownHostException
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


fun List<ClosedRange<ULong>>.toFlatList(): List<ULong> = asFlatSequence { it + 1UL }.toList()

@OptIn(ExperimentalTime::class, ExperimentalStdlibApi::class)
class RangeListTest : FunSpec({

    test("toRangeLSet-1") {
        shouldThrow<Exception> { "".toIpV4ULong() }
        shouldThrow<UnknownHostException> { "a".toIpV4ULong() }
        "".toIpV4RangeSet().toFlatList() shouldBe listOf()
        "0".toIpV4RangeSet().toFlatList() shouldBe listOf("0.0.0.0".toIpV4ULong()) // no class
        "1.2".toIpV4RangeSet().toFlatList() shouldBe listOf("1.0.0.2".toIpV4ULong()) // class A
        "1.65536".toIpV4RangeSet().toFlatList() shouldBe listOf("1.1.0.0".toIpV4ULong()) // class A
        "1.2.3".toIpV4RangeSet().toFlatList() shouldBe listOf("1.2.0.3".toIpV4ULong()) // class B
        "1.2.256".toIpV4RangeSet().toFlatList() shouldBe listOf("1.2.1.0".toIpV4ULong()) // class B
        "1.2.3.4".toIpV4RangeSet().toFlatList() shouldBe listOf("1.2.3.4".toIpV4ULong()) // class C
        "65536".toIpV4RangeSet().toFlatList() shouldBe listOf("0.1.0.0".toIpV4ULong()) // no class
    }
    test("toRangeSet-2") {
        "".toIpV4RangeSet().toFlatList() shouldBe listOf()
        "0".toIpV4RangeSet().toFlatList() shouldBe listOf(0UL)
        "0,0".toIpV4RangeSet().toFlatList() shouldBe listOf(0UL)
        "0,2".toIpV4RangeSet().toFlatList() shouldBe listOf(0UL, 2UL)
        "0,1,255".toIpV4RangeSet().toFlatList() shouldBe listOf(0UL, 1UL, 255UL)
        "1,,2".toIpV4RangeSet().toFlatList() shouldBe listOf(1UL, 2UL)
        ",,1,,,,2,".toIpV4RangeSet().toFlatList() shouldBe listOf(1UL, 2UL)
        "4,2,6".toIpV4RangeSet().toFlatList() shouldBe listOf(2UL, 4UL, 6UL)

    }
    test("toRangeSet-3") {
        "".toIpV4RangeSet().asFlatSequence().toList() shouldBe listOf()
        "1-3".toIpV4RangeSet().toFlatList() shouldBe listOf(1UL, 2UL, 3UL)
        "1-1".toIpV4RangeSet().toFlatList() shouldBe listOf(1UL)
        "3-1".toIpV4RangeSet().toFlatList() shouldBe listOf() // reversed is empty
        "1-3,5-7".toIpV4RangeSet().toFlatList() shouldBe listOf(1UL, 2UL, 3UL, 5UL, 6UL, 7UL)
        "1-3,4-5".toIpV4RangeSet() shouldBe "1-5".toIpV4RangeSet()
        "1-3,4,5,6-7,0".toIpV4RangeSet() shouldBe "0-7".toIpV4RangeSet()

        "0,1-3,0.0.1.1-0.0.1.3".toIpV4RangeSet().toFlatList() shouldBe listOf(
            0UL,
            1UL,
            2UL,
            3UL,
            "0.0.1.1".toIpV4ULong(),
            "0.0.1.2".toIpV4ULong(),
            "0.0.1.3".toIpV4ULong()
        )
    }
    test("slicedFlow2") {
        "0,1-3,0.0.1.1-0.0.1.3".toIpV4RangeSet().asFlatSequence().toList() shouldBe listOf(
            0UL,
            1UL,
            2UL,
            3UL,
            "0.0.1.1".toIpV4ULong(),
            "0.0.1.2".toIpV4ULong(),
            "0.0.1.3".toIpV4ULong()
        )

        val src = "0,1-3,0.0.1.1-0.0.1.3".toIpV4RangeSet()

        src.toFlatList().also(::println) shouldBe listOf(
            0UL,
            1UL,
            2UL,
            3UL,
            "0.0.1.1".toIpV4ULong(),
            "0.0.1.2".toIpV4ULong(),
            "0.0.1.3".toIpV4ULong(),
        )
        src.toFlatList().also(::println) shouldBe listOf(
            "0",
            "2",
            "0.0.1.2",
            "1",
            "3",
            "0.0.1.1",
            "0.0.1.3"
        ).map { it.toIpV4ULong() }
        src.toFlatList().also(::println) shouldBe listOf(
            "0",
            "2",
            "0.0.1.2",
            "1",
            "3",
            "0.0.1.1",
            "0.0.1.3"
        ).map { it.toIpV4ULong() }
    }
})

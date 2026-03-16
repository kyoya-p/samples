package jp.wjg.shokkaa.snmp

import jp.wjg.shokkaa.snmp.asFlatSequence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

typealias RangeSet<T> = Collection<ClosedRange<T>>
typealias IpV4RangeSet = Collection<ClosedRange<UInt>>

fun <T : Comparable<T>> RangeSet<T>.toRangeList(): List<ClosedRange<T>> {
    if (this.isEmpty()) return emptyList()

    val sortedRanges = this.sortedBy { it.start }
    val result = mutableListOf<ClosedRange<T>>()

    for (currentRange in sortedRanges) {
        if (currentRange.isEmpty()) continue
        if (result.isEmpty() || result.last().endInclusive < currentRange.start) {
            result.add(currentRange)
        } else {
            val lastRange = result.removeLast()
            val mergedStart = minOf(lastRange.start, currentRange.start)
            val mergedEnd = maxOf(lastRange.endInclusive, currentRange.endInclusive)
            result.add(mergedStart..mergedEnd)
        }
    }
    return result
}

fun <T : Comparable<T>> Collection<ClosedRange<T>>.asFlatSequence(inc: (T) -> T): Sequence<T> =
    asSequence().flatMap { r ->
        sequence {
            var p = r.start
            while (p <= r.endInclusive) {
                yield(p)
                p = inc(p)
            }
        }
    }


fun String.toIpV4Range() = split("-").map { it.toIpV4UInt() }.let { it[0]..if (it.size == 1) it[0] else it[1] }
fun String.toIpV4RangeSet() =
    split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { it.toIpV4Range() }.toRangeList()

fun IpV4RangeSet.asUIntFlatSequence() = asFlatSequence { it + 1u }
fun IpV4RangeSet.totalLength() = sumOf { it.endInclusive - it.start + 1u }

fun Flow<UInt>.scrambled(nBits: Int): Flow<UInt> = flow {
    val mask = (1U shl nBits) - 1U
    val rndTable = (0U..mask).shuffled()
    for (w in 0U..mask) collect { ip -> if (ip and mask == rndTable[w.toInt()]) emit(ip) }
}


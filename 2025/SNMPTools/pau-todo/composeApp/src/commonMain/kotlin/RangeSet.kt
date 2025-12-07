package jp.wjg.shokkaa.snmp

typealias RangeSet<T> = Collection<ClosedRange<T>>

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

typealias UIntRangeSet = Collection<ClosedRange<UInt>>

fun UIntRangeSet.totalLength() = sumOf { it.endInclusive - it.start + 1u }
fun UIntRangeSet.asFlatSequence(): Sequence<UInt> = asFlatSequence<UInt>(inc = { it + 1u })

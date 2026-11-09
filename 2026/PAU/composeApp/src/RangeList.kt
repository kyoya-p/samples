package jp.wjg.shokkaa.snmp

typealias RangeSet<T> = Collection<OpenEndRange<T>>

fun <T : Comparable<T>> RangeSet<T>.toRangeList(): List<OpenEndRange<T>> {
    if (this.isEmpty()) return emptyList()

    val sortedRanges = this.sortedBy { it.start }
    val result = mutableListOf<OpenEndRange<T>>()

    for (currentRange in sortedRanges) {
        if (currentRange.isEmpty()) continue
        if (result.isEmpty() || result.last().endExclusive < currentRange.start) {
            result.add(currentRange)
        } else {
            val lastRange = result.removeLast()
            val mergedStart = minOf(lastRange.start, currentRange.start)
            val mergedEnd = maxOf(lastRange.endExclusive, currentRange.endExclusive)
            result.add(mergedStart..<mergedEnd)
        }
    }
    return result
}


fun <T : Comparable<T>> Collection<OpenEndRange<T>>.asFlatSequence(inc: (T) -> T): Sequence<T> = asSequence().flatMap { r ->
    sequence {
        var p = r.start
        while (p < r.endExclusive) {
            yield(p)
            p = inc(p)
        }
    }
}

typealias IpV4RangeSet = Collection<OpenEndRange<ULong>>

fun <T : Comparable<T>> openRange(start: T, end: T): OpenEndRange<T> = start..<end

fun String.toIpV4Range(): OpenEndRange<ULong> = split("-").map { it.toIpV4ULong() }.let {
    openRange(it[0], if (it.size == 1) it[0] + 1UL else it[1] + 1UL)
}

fun String.toIpV4RangeSet(): List<OpenEndRange<ULong>> =
    split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { it.toIpV4Range() }.toRangeList()

fun IpV4RangeSet.asFlatSequence(): Sequence<ULong> = asFlatSequence<ULong>(inc = { it + 1u })
fun IpV4RangeSet.totalLength(): ULong = sumOf { it.endExclusive - it.start }



typealias RangeSet<T> = List<ClosedRange<T>>

fun <T : Comparable<T>> rangeSetOf(src: List<ClosedRange<T>>) = src.fold(listOf<ClosedRange<T>>()) { a, e -> a.plus(e) }
fun <T : Comparable<T>> RangeSet<T>.plus(r: ClosedRange<T>): RangeSet<T> = flatMap { ex ->
    when {
        ex.endInclusive < r.start -> listOf(ex)
        r.endInclusive < ex.start -> listOf(r, ex)
//todo
        else -> listOf(ex)
    }
}

typealias OpenEndRangeList<T> = List<OpenEndRange<T>>


// s1..<e1 と s2..<e2 の和結合を返す
fun <T : Comparable<T>> OpenEndRange<T>.union(o: OpenEndRange<T>) = when {
    endExclusive < o.start -> listOf(this, o)
    o.endExclusive < start -> listOf(o, this)
    else -> listOf(minOf(start, o.start)..<maxOf(endExclusive, o.endExclusive))
}

fun <T : Comparable<T>> OpenEndRangeList<T>.union(o: OpenEndRange<T>) {
    var n = o
//    windowed(2, partialWindows = true).flatMap {
//
//    }
}

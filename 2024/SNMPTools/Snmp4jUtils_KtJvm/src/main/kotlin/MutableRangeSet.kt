package jp.wjg.shokkaa.snmp4jutils

import java.util.*


abstract class MutableRangeSet<T : Comparable<T>> : MutableSet<ClosedRange<T>>, Cloneable {
    private val ranges = LinkedList<ClosedRange<T>>()

    constructor()
    constructor(ranges: List<ClosedRange<T>>) {
        addAll(ranges)
    }

    protected constructor(rangeSet: MutableRangeSet<T>) {
        ranges.addAll(rangeSet)
    }

    constructor(p0: Any)

    override val size: Int
        get() = ranges.size

    fun containsValue(value: T): Boolean = ranges.any { it.contains(value) }

    override fun contains(element: ClosedRange<T>) =
        ranges.any { r -> element.start.compareTo(r.start) >= 0 && element.endInclusive.compareTo(r.endInclusive) <= 0 }

//    override fun contains(element: ClosedRange<T>): Boolean {
//        for (range in ranges)
//            if (element.start.compareTo(range.start) >= 0 && element.endInclusive.compareTo(range.endInclusive) <= 0)
//                return true
//        return false
//    }

    override fun containsAll(elements: Collection<ClosedRange<T>>): Boolean = elements.all { contains(it) }

    override fun isEmpty(): Boolean = ranges.isEmpty()

    override fun add(element: ClosedRange<T>): Boolean {
        var new = element
        if (new.start > new.endInclusive) return false
        var addIndex = -1

        val iterator = ranges.iterator()
        while (iterator.hasNext()) {
            val existing = iterator.next()
            addIndex++

            if (new.endInclusive.compareTo(decrementValue(existing.start)) < 0) {
                ranges.add(addIndex, new)
                return true
            }

            if (new.start.compareTo(existing.start) < 0) {
                if (new.endInclusive.compareTo(existing.endInclusive) < 0)
                    new = createRange(new.start, existing.endInclusive)
                iterator.remove()
                addIndex--
                continue
            }

            if (new.endInclusive.compareTo(existing.endInclusive) <= 0) return false

            if (new.start.compareTo(incrementValue(existing.endInclusive)) <= 0) {
                new = createRange(existing.start, new.endInclusive)
                iterator.remove()
                addIndex--
            }
        }

        ranges.add(new)
        return true
    }

    override fun addAll(elements: Collection<ClosedRange<T>>) = elements.map { add(it) }.any()
    override fun clear() = ranges.clear()
    override fun remove(element: ClosedRange<T>): Boolean {
        var changed = false

        val iterator = ranges.listIterator()
        while (iterator.hasNext()) {
            val existing = iterator.next()

            if (element.endInclusive.compareTo(existing.start) < 0)
                break

            if (element.start.compareTo(existing.endInclusive) > 0)
                continue

            val removeFromStart = element.start.compareTo(existing.start) <= 0
            val removeFromEnd = element.endInclusive.compareTo(existing.endInclusive) >= 0
            iterator.remove()
            changed = true

            if (!removeFromStart)
                iterator.add(createRange(existing.start, decrementValue(element.start)))

            if (!removeFromEnd)
                iterator.add(createRange(incrementValue(element.endInclusive), existing.endInclusive))
        }

        return changed
    }

    override fun removeAll(elements: Collection<ClosedRange<T>>) = elements.map { remove(it) }.any { it }
    fun retain(element: ClosedRange<T>): Boolean {
        var changed = false

        val iterator = ranges.listIterator()
        while (iterator.hasNext()) {
            val existing = iterator.next()

            val removeFromStart = element.start.compareTo(existing.start) > 0
            val removeFromEnd = element.endInclusive.compareTo(existing.endInclusive) < 0

            if (!removeFromStart && !removeFromEnd)
                continue

            iterator.remove()
            changed = true

            if (element.endInclusive.compareTo(existing.start) < 0) {
                iterator.forEach { iterator.remove() }
                break
            }

            if (element.start.compareTo(existing.endInclusive) > 0)
                continue

            iterator.add(
                createRange(
                    if (removeFromStart) element.start else existing.start,
                    if (removeFromEnd) element.endInclusive else existing.endInclusive
                )
            )
        }

        return changed
    }

    override fun retainAll(elements: Collection<ClosedRange<T>>): Boolean {

        val unnormalizedRanges = elements.map { element ->
            val clone = clone()
            clone.retain(element)
            clone.ranges
        }

        val shallowRangesCopy = LinkedList(ranges)

        clear()
        unnormalizedRanges.forEach { addAll(it) }

        return shallowRangesCopy != ranges
    }

    fun difference(element: ClosedRange<T>) = differenceAll(listOf(element))
    fun differenceAll(elements: Collection<ClosedRange<T>>): MutableRangeSet<T> {
        val difference = clone()
        difference.clear()
        difference.ranges.addAll(elements)
        difference.removeAll(ranges)
        return difference
    }

    fun gaps(): MutableRangeSet<T> {
        return if (ranges.isEmpty()) clone().apply { clear() } else difference(ranges.first.endInclusive..ranges.last.start)
    }

    override fun iterator(): MutableIterator<ClosedRange<T>> = ranges.iterator()

    override fun hashCode(): Int {
        return ranges.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (other is MutableRangeSet<*> && ranges == other.ranges)
    }

    protected abstract fun createRange(start: T, endInclusive: T): ClosedRange<T>

    protected abstract fun incrementValue(value: T): T

    protected abstract fun decrementValue(value: T): T

    override abstract fun clone(): MutableRangeSet<T>

    infix operator fun plusAssign(o: ClosedRange<T>) {
        add(o)
    }


    infix operator fun plusAssign(o: Collection<ClosedRange<T>>) {
        addAll(o)
    }

    infix operator fun minusAssign(o: ClosedRange<T>) {
        remove(o)
    }

    infix operator fun minusAssign(o: Collection<ClosedRange<T>>) {
        removeAll(o)
    }
}


class IntRangeSet : MutableRangeSet<Int> {
    constructor() : super()
    constructor(ranges: List<IntRange>) : super(ranges)
    constructor(vararg ranges: IntRange) : this(ranges.asList())
    private constructor(rangeSet: IntRangeSet) : super(rangeSet)

    override fun createRange(start: Int, endInclusive: Int): IntRange = IntRange(start, endInclusive)
    override fun incrementValue(value: Int): Int = value + 1
    override fun decrementValue(value: Int): Int = value - 1
    override fun clone(): MutableRangeSet<Int> = IntRangeSet(this)
}


class ULongRangeSet : MutableRangeSet<ULong> {
    constructor() : super()
    constructor(ranges: List<ULongRange>) : super(ranges)
    constructor(vararg ranges: ULongRange) : this(ranges.asList())
    private constructor(rangeSet: ULongRangeSet) : super(rangeSet)

    override fun createRange(start: ULong, endInclusive: ULong): ULongRange = ULongRange(start, endInclusive)
    override fun incrementValue(value: ULong): ULong = value + 1UL
    override fun decrementValue(value: ULong): ULong = value - 1UL
    override fun clone(): MutableRangeSet<ULong> = ULongRangeSet(this)
}

fun ClosedRange<Int>.length() = if (isEmpty()) 0 else endInclusive - start + 1
fun IntRangeSet.totalLength() = sumOf { it.length() }

fun ClosedRange<ULong>.length() = if (isEmpty()) 0UL else (endInclusive - start + 1UL)
fun ULongRangeSet.totalLength() = sumOf { it.length() }

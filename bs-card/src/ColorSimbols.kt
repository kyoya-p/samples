package BSSim

inline class Color(val b: UByte) : Comparable<Color> { // [God,U,B,Y,W,G,P,R]
    companion object {
        inline val None get() = Color(0b0000_0000u)
        inline val R get() = Color(0b0000_0001u)
        inline val P get() = Color(0b0000_0010u)
        inline val G get() = Color(0b0000_0100u)
        inline val W get() = Color(0b0000_1000u)
        inline val Y get() = Color(0b0001_0000u)
        inline val B get() = Color(0b0010_0000u)
        inline val Ut get() = Color(0b0100_0000u)
        inline val Gd get() = Color(0b1000_0000u)
        inline val RPGWYB get() = R + P + G + W + Y + B

    }

    override fun compareTo(o: Color): Int = b.toInt() - o.b.toInt()

    inline fun colorCount(): Int {
        var i = b.toUInt()
        i = i - (i shr 1 and 0x55555555u)
        i = (i and 0x33333333u) + (i shr 2 and 0x33333333u)
        i = i + (i shr 4) and 0x0f0f0f0fu
        i = i + (i shr 8)
        i = i + (i shr 16)
        return i.toInt() and 0x3f
    }

    inline operator fun plus(o: Color) = Color(b or o.b)
    inline operator fun times(o: Color) = Color(b and o.b)
    inline fun matches(o: Color): Boolean = ((b and o.b) != 0.toUByte())
}


inline class Sbl(val s: Map<Color, Int> = mapOf()) : Comparable<Sbl> {
    constructor(s: Color, v: Int = 1) : this(mapOf(s to v))

    override fun compareTo(o: Sbl): Int {
        val a = s.toSortedMap().toList()
        val b = o.s.toSortedMap().toList()
        if (a.size != b.size) return a.size - b.size
        val r = a.zip(b).lastOrNull { (a, b) ->
            a != b
        } ?: return 0
        return r.first.first.compareTo(r.second.first)
                .if0 { r.first.second.compareTo(r.second.second) }
    }

    inline fun Map<Color, Int>.toSimbols() = Sbl(this)
    inline fun Color.toSimbols() = Sbl(this)
    inline fun Pair<Color, Int>.toSimbols() = Sbl(mapOf(this))
    inline operator fun plus(b: Sbl) = Sbl(s + b.s.keys.associateWith { k -> (s[k] ?: 0) + (b.s[k] ?: 0) })
    inline operator fun times(b: Int) = s.map { (c, v) -> c to v * b }.toMap().toSimbols()
    inline fun matches(pickColor: Color) = s.asSequence().filter { (c, v) -> pickColor.matches(c) }
    inline fun toInt(): Int = s.values.fold(0) { a, t -> a + t }

    inline fun reduction(fieldSimbols: Sbl): Int {  // [TODO]最適解ではない
        var fs = fieldSimbols.s.toMutableMap()
        return s.asSequence().flatMap { (c, v) -> (1..v).map { c }.asSequence() }.filter { c ->
            val x = fs[c]
            if (x != null) {
                val y = x - 1
                when {
                    y == 0 -> fs.remove(c)
                    else -> fs[c] = y
                }
                true
            } else {
                false
            }
        }.count()
    }

    companion object {
        inline val Zero get() = Sbl()
        inline val R get() = Sbl(mapOf(Color.R to 1))
        inline val P get() = Sbl(mapOf(Color.P to 1))
        inline val G get() = Sbl(mapOf(Color.G to 1))
        inline val W get() = Sbl(mapOf(Color.W to 1))
        inline val Y get() = Sbl(mapOf(Color.Y to 1))
        inline val B get() = Sbl(mapOf(Color.B to 1))
        inline val Ut get() = Sbl(mapOf(Color.Ut to 1))
        inline val Gd get() = Sbl(mapOf(Color.Gd to 1))

        fun test() {
            (Sbl.R * 3).reduction(Sbl.R).assert(1)
            (Sbl.R * 3).reduction(Sbl.R * 4).assert(3)
            (Sbl.R * 3 + Sbl.G).reduction(Sbl.R * 2 + Sbl.G * 2).assert(3)
        }
    }

}

package BSSim

open class Core(val c/*総数*/: Int, val s/*うちソウルコア数 core>=sCore*/: Int = 0) : Comparable<Core> {
    constructor(core: Core) : this(core.c, core.s)

    operator fun plus(o: Core) = Core(c + o.c, s + o.s)
    operator fun minus(o: Core) = Core(c - o.c, s - o.s)
    operator fun unaryMinus() = Core(-c, -s)
    override fun toString(): String = "${c}" + when {
        (s >= 1) -> ".${s}"
        else -> ""
    }

    override fun hashCode(): Int {
        return c * 31 * 31 + s
    }

    override fun equals(o: Any?): Boolean {
        if (o is Core) return c == o.c && s == o.s
        else return false
    }

    // スカラ値でないので不正確(soredSetのため)
    override operator fun compareTo(o: Core): Int {
        if (c != o.c) return c - o.c
        else return s - o.s
    }

    fun repeat() = downTo(Core(0))
    fun downTo(end: Core): Sequence<Core> = sequence<Core> {
        fun min(a: Int, b: Int) = if (a >= b) b else a
        for (c in c downTo end.c) for (sc in c downTo end.s) { // Sコアをピックアップすることもあり
            yield(Core(c, sc))
        }
    }

    fun toInt() = c

    companion object {
        fun test() {
            val C0 = Core(0, 0)
            val C0x = Core(0, 0)
            val C1 = Core(1, 0)
            val C11 = Core(1, 1)
            val C2 = Core(2, 0)
            val C21 = Core(2, 1)
            val C22 = Core(2, 2)
            val C3 = Core(3, 0)
            val C31 = Core(3, 1)
            val C32 = Core(3, 2)
            val C33 = Core(3, 3)
            val C4 = Core(4)
            val C42 = Core(4, 2)
            val C43 = Core(4, 3)
            val C44 = Core(4, 4)

            C0.assert(C0x)

            C2.downTo(C1).toList().assert(listOf(C22, C21, C2, C11, C1))//sCore0個以上
            C2.downTo(C11).toList().assert(listOf(C22, C21, C11))//sCore1個以上
            C3.downTo(C22).toList().assert(listOf(C33, C32, C22))

            C21.pickCore(C0).toList().assert(listOf(C0 to C21))
            C21.pickCore(C1).toList().assert(listOf(C1 to C11))
            C21.pickCore(C11).toList().assert(listOf(C11 to C1))
            C21.pickCore(C2).toList().assert(listOf<Pair<Core, Core>>())
            C21.pickCore(C21).toList().assert(listOf(C21 to C0))
            C21.pickCore(C22).toList().assert(listOf<Pair<Core, Core>>())

            C0.pickCore(0).toList().assert(listOf(C0 to C0))
            C2.pickCore(0).toList().assert(listOf(C0 to C2))
            C2.pickCore(1).toList().assert(listOf(C1 to C1))
            C2.pickCore(2).toList().assert(listOf(C2 to C0))
            C21.pickCore(0).toList().assert(listOf(C0 to C21))
            C21.pickCore(1).toList().assert(listOf(C1 to C11, C11 to C1))
            C21.pickCore(2).toList().assert(listOf(C21 to C0))
            C21.pickCore(3).toList().assert(listOf<Pair<Core, Core>>())

            C21.pickCore(C21, C21).toList().assert(listOf(C21 to C0))
            C21.pickCore(C21, C1).toList().assert(listOf(C21 to C0, C11 to C1, C1 to C11))
            C21.pickCore(C21, C0).toList().assert(listOf(C21 to C0, C11 to C1, C1 to C11, C0 to C21))
            C2.pickCore(C11, C11).toList().assert(listOf<Pair<Core, Core>>())

            C2.pickCore().toList().assert(listOf(C2 to C0, C1 to C1, C0 to C2))
            C21.pickCore().toList().assert(listOf(C21 to C0, C1 to C11, C11 to C1, C0 to C21))

            listOf(C4).pickCore(3).map { (_, lc) -> lc }.toList().assert(listOf(listOf(C1)))
            listOf(C1, C2, C3).pickCore(0).map { (_, lc) -> lc }.toList().assert(listOf(listOf(C1, C2, C3)))
            listOf(C2, C2).pickCore(1).map { (_, lc) -> lc }.toList().assert(listOf(listOf(C1, C2), listOf(C2, C1)))
            listOf(C0, C2).pickCore(3).map { (_, lc) -> lc }.toList().assert(listOf<List<Core>>())
            listOf(C2, C2).pickCore(2).map { (_, lc) -> lc }.toList().assert(listOf(listOf(C0, C2), listOf(C1, C1), listOf(C2, C0)))
            listOf(C21, C2).pickCore(1).map { (_, lc) -> lc }.toList().assert(listOf(listOf(C11, C2), listOf(C1, C2), listOf(C21, C1)))

        }
    }
}

// 一つのコアフォルダから指定コアを取り出す(Sコアも厳密に一致させる)
// 取り出すことができれば、取り出したコアと残りのコアの組合せ(空か1個)を返す
fun Core.pickCore(pick: Core): Sequence<Pair<Core, Core>> {
    val r = this - pick
    if (r.c >= 0 && r.s >= 0 && r.c >= r.s) return sequenceOf(pick to r)
    else return sequenceOf()
}

// 一つのコアフォルダから指定コアを取り出す(取り出すSコアは任意)
// 取り出すことができれば、取り出したコアと残りのコアの組合せを返す
fun Core.pickCore(pick: Int): Sequence<Pair<Core, Core>> {
    fun min(a: Int, b: Int) = if (a > b) b else a
    fun max(a: Int, b: Int) = if (a > b) a else b
    val r = this.c - pick
    if (r < 0) return sequenceOf()
    return (max(0, s - (c - pick))..min(pick, s)).asSequence()
            .map { scPicked ->
                Core(pick, scPicked)
            }
            .map { cPicked ->
                cPicked to (this - cPicked)
            }
}

// 一つのコアフォルダからコアを任意個取り出す(取り出すSコアは任意)
// 取り出したコアと残りのコアの組合せを返す
fun Core.pickCore(): Sequence<Pair<Core, Core>> = (c downTo 0).asSequence().flatMap { pickCore(it) }

// 一つのコアフォルダからmin個以上、max個以下を取り出す
// 取り出したコアと残りのコアの組合せパターンを返す
fun Core.pickCore(pick: Core, minPick: Core): Sequence<Pair<Core, Core>> = sequence {
    fun min(a: Int, b: Int) = if (a > b) b else a
    val holder = this@pickCore
    for (c in pick.c downTo minPick.c) for (sc in min(c, pick.s) downTo minPick.s) {
        val p = Core(c, sc)
        holder.pickCore(p).forEach {
            yield(it)
        }
    }
}

// 複数個のコアホルダーから指定のコアを取り出す組み合わせ
// 取り出したコアと残りのコアのパターンを返す
fun List<Core>.pickCore(pick: Core): Sequence<Pair<Core, List<Core>>> {
    fun min(a: Int, b: Int) = if (a > b) b else a
    return if (size == 0) {
        if (pick.c == 0) sequenceOf(Core(0) to listOf<Core>())
        else sequenceOf()
    } else {
        val topCoreHolder = this[0]
        val remCoreHolders = drop(1)
        sequence {
            for (c in pick.c downTo 0) for (sc in min(c, pick.s) downTo 0) {
                val pick1st = Core(c, sc)
                topCoreHolder.pickCore(pick1st).forEach { (picked1st, r1st) ->
                    remCoreHolders.pickCore(pick - picked1st).forEach { (rPicked, rCores) ->
                        yield((picked1st + rPicked) to (listOf(r1st) + rCores))
                    }
                }
            }
        }
    }
}

// 複数個のコアホルダーから指定個のコア(任意個のSコア)を取り出す組み合わせ
// 取り出したコアと残りのコアのパターンを返す
fun List<Core>.pickCore(pick: Int): Sequence<Pair<Core, List<Core>>> =
        if (size == 0) {
            if (pick == 0) sequenceOf(Core(0) to listOf<Core>())
            else sequenceOf()
        } else {
            val topCoreHolder = this[0]
            val subList = drop(1)
            sequence {
                for (pick1st in pick downTo 0) {
                    topCoreHolder.pickCore(pick1st).forEach { (picked1st, cores1st) ->
                        subList.pickCore(pick - pick1st).forEach { (rPicked, rCores) ->
                            yield((picked1st + rPicked) to (listOf(cores1st) + rCores))
                        }
                    }
                }
            }
        }

// 複数個のコアホルダーから任意個のコア(任意個のSコア)を取り出す組み合わせ
// 取り出したコアと残りのコアのパターンを返す






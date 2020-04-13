package BSSim

data class Situation(
        val ownSide: Side,
        val enemySide: Side = Side(),
        val step: Int = 1
) {
    constructor(prevStn: Situation
                , ownSide: Side = prevStn.ownSide
                , enemySide: Side = prevStn.enemySide
                , step: Int = prevStn.step
    ) : this(ownSide = ownSide, enemySide = enemySide, step = step)

    // -----New API ----------------------------------------------------------
    inline fun tr(ownSide: Side = this.ownSide
                  , enemySide: Side = this.enemySide
                  , step: Int = this.step) = Situation(ownSide = ownSide, enemySide = enemySide, step = step)

    inline fun trOwnSide(op: Side.() -> Side): Situation = tr(ownSide = ownSide.op())
    inline fun trOwnSideSq(op: Side.() -> Sequence<Side>): Sequence<Situation> = ownSide.op().map { tr(ownSide = it) }

    inline fun trEnemySide(op: Side.() -> Side): Situation = tr(enemySide = enemySide.op())
    inline fun trEnemySideSq(op: Side.() -> Sequence<Side>): Sequence<Situation> = enemySide.op().map { tr(ownSide = it) }

    override fun toString() = "St{${ownSide}}"
}

fun <T> Sequence<T>.onlyTakeOneCase(): T = take(1).toList()[0]

// Transitionシーケンス中にSituationシーケンスをはさむ
fun Sequence<Transition>.sqSituation(op: Sequence<Situation>.() -> Sequence<Situation>): Sequence<Transition> = flatMap { tr -> sequenceOf(tr.stn).op().map { Transition(prevStn = tr.prevStn, stn = it) } }
fun Sequence<Transition>.sqSituation_flatMap(d: UInt = 0u, op: Situation.() -> Sequence<Situation>): Sequence<Transition> = this.sqSituation { flatMap { it.op() } }
fun Sequence<Transition>.sqSituation_map(d: UInt = 0u, op: Situation.() -> Situation): Sequence<Transition> = this.sqSituation { map { it.op() } }

fun Sequence<Transition>.sqOwnSide(d: UInt = 0u, op: Sequence<Side>.() -> Sequence<Side>): Sequence<Transition> = this.sqSituation { sqOwnSide { op() } }
fun Sequence<Transition>.sqOwnSide_flatMap(d: UInt = 0u, op: Side.() -> Sequence<Side>): Sequence<Transition> = this.sqSituation { sqOwnSide { flatMap { it.op() } } }
fun Sequence<Transition>.sqOwnSide_map(d: UInt = 0u, op: Side.() -> Side): Sequence<Transition> = this.sqSituation { sqOwnSide { map { it.op() } } }

fun Sequence<Transition>.sqOwnSide_filter(d: UInt = 0u, op: Side.() -> Boolean): Sequence<Transition> = this.sqSituation { sqOwnSide { filter { it.op() } } }

fun Sequence<Situation>.sqOwnSide(d: Int = 0, op: Sequence<Side>.() -> Sequence<Side>): Sequence<Situation> = flatMap { s -> sequenceOf(s.ownSide).op().map { s.tr(ownSide = it) } }


// 状態遷移(効果)
data class Transition(val prevStn: Situation, val stn: Situation)


fun main() {

}


package BSSim

data class World(
        val ownSide: Side,
        val enemySide: Side = Side(),
        val step: Int = 1
) {
    constructor(prevStn: World
                , ownSide: Side = prevStn.ownSide
                , enemySide: Side = prevStn.enemySide
                , step: Int = prevStn.step
    ) : this(ownSide = ownSide, enemySide = enemySide, step = step)

    // -----New API ----------------------------------------------------------
    inline fun tr(ownSide: Side = this.ownSide
                  , enemySide: Side = this.enemySide
                  , step: Int = this.step) = World(ownSide = ownSide, enemySide = enemySide, step = step)

    inline fun trOwnSide(op: Side.() -> Side): World = tr(ownSide = ownSide.op())
    inline fun trOwnSideSq(op: Side.() -> Sequence<Side>): Sequence<World> = ownSide.op().map { tr(ownSide = it) }

    inline fun trEnemySide(op: Side.() -> Side): World = tr(enemySide = enemySide.op())
    inline fun trEnemySideSq(op: Side.() -> Sequence<Side>): Sequence<World> = enemySide.op().map { tr(ownSide = it) }

    override fun toString() = "St{${ownSide}}"
}

fun <T> Sequence<T>.onlyTakeOneCase(): T = take(1).toList()[0]

// Transitionシーケンス中にSituationシーケンスをはさむ
fun Sequence<SpaceTime>.sqSituation(op: Sequence<World>.() -> Sequence<World>): Sequence<SpaceTime> = flatMap { tr -> sequenceOf(tr.stn).op().map { SpaceTime(prevStn = tr.prevStn, stn = it) } }
fun Sequence<SpaceTime>.sqSituation_flatMap(d: UInt = 0u, op: World.() -> Sequence<World>): Sequence<SpaceTime> = this.sqSituation { flatMap { it.op() } }
fun Sequence<SpaceTime>.sqSituation_map(d: UInt = 0u, op: World.() -> World): Sequence<SpaceTime> = this.sqSituation { map { it.op() } }

fun Sequence<SpaceTime>.sqOwnSide(d: UInt = 0u, op: Sequence<Side>.() -> Sequence<Side>): Sequence<SpaceTime> = this.sqSituation { sqOwnSide { op() } }
fun Sequence<SpaceTime>.sqOwnSide_flatMap(d: UInt = 0u, op: Side.() -> Sequence<Side>): Sequence<SpaceTime> = this.sqSituation { sqOwnSide { flatMap { it.op() } } }
fun Sequence<SpaceTime>.sqOwnSide_map(d: UInt = 0u, op: Side.() -> Side): Sequence<SpaceTime> = this.sqSituation { sqOwnSide { map { it.op() } } }

fun Sequence<SpaceTime>.sqOwnSide_filter(d: UInt = 0u, op: Side.() -> Boolean): Sequence<SpaceTime> = this.sqSituation { sqOwnSide { filter { it.op() } } }

fun Sequence<World>.sqOwnSide(d: Int = 0, op: Sequence<Side>.() -> Sequence<Side>): Sequence<World> = flatMap { s -> sequenceOf(s.ownSide).op().map { s.tr(ownSide = it) } }


// 状態遷移
data class SpaceTime(val prevStn: World, val stn: World)


fun main() {

}


package BSSim

data class World(
        val ownSide: Side,
        val enemySide: Side = Side(),
        val step: Int = 0
) {
    /*    constructor(prevStn: World
                    , ownSide: Side = prevStn.ownSide
                    , enemySide: Side = prevStn.enemySide
                    , step: Int = prevStn.step
        ) : this(ownSide = ownSide, enemySide = enemySide, step = step)
    */

    // -----New API ----------------------------------------------------------
    inline fun tr(ownSide: Side = this.ownSide
                  , enemySide: Side = this.enemySide
                  , step: Int = this.step) = World(ownSide = ownSide, enemySide = enemySide, step = step)

    override fun toString() = "Wd{s:${step} ${ownSide}}"
}

fun <T> Sequence<T>.onlyTakeOneCase(msg: String = ""): T {
    try {
        return take(1).toList()[0]
    } catch (e: Exception) {
        throw Exception("onlyTakeOneCase():" + msg)
    }
}

fun ParallelWorld.flatMap_world(op: World.() -> Sequence<World>): ParallelWorld = flatMap { h -> h.world.op().map { h.tr(it) } }
fun ParallelWorld.flatMap_ownSide(d: UInt = 0u, op: Side.() -> Sequence<Side>): ParallelWorld = flatMap_world { ownSide.op().map { tr(ownSide = it) } }

fun ParallelWorld.map_world(op: World.() -> World): ParallelWorld = map { h -> h.tr(h.world.op()) }
fun ParallelWorld.map_ownSide(d: UInt = 0u, op: Side.() -> Side): ParallelWorld = map_world { tr(ownSide = ownSide.op()) }

fun ParallelWorld.filter_ownSide(cond: Side.() -> Boolean): ParallelWorld = filter { it.world.ownSide.cond() }
fun ParallelWorld.if_world(cond: World.() -> Boolean, op: ParallelWorld.() -> ParallelWorld): ParallelWorld = sequence {
    flatMap {
        if (it.world.cond()) sequenceOf(it).op()
        else sequenceOf(it)
    }
}

infix fun ParallelWorld.effect(e: Maneuver): ParallelWorld = flatMap { e.use(it) } //～を発揮する
fun ParallelWorld.optional(op: ParallelWorld.() -> ParallelWorld): ParallelWorld = sequence {//～できる
    forEach {
        yield(it)// 何もしない場合
    }
    op().forEach {
        yield(it) // 操作を行った結果
    }
}

// 状態遷移
data class History constructor(val past: History?, val world: World) {
    inline fun tr(newWorld: World): History = History(past = null, world = newWorld)

    override fun toString(): String = "past:${past.hashCode()} world={${world}}"

    companion object {
        fun eden(deck: Set<Card>, enemyDeck: Set<Card>): ParallelWorld = sequenceOf(History(null, World(ownSide = initialSide(deck), enemySide = initialSide(enemyDeck), step = 0)))
    }
}

typealias ParallelWorld = Sequence<History>


// すべての効果を再帰的に実行
class eMainStep : Maneuver {
    override val efName: String = "MainStep"

    override fun use(h: History): ParallelWorld = useChecked(h, 0)
    //sequence {
    /*yield(h) //何もしない選択
    h.stn.ownSide.hand.cards.forEach {
        it.use(h).forEach {
            this@eMainStep.use(it).forEach { yield(it) }
        }
    }*/


    fun useChecked(h: History, depth: Int): ParallelWorld = sequence {
        if (depth >= 1024) {
            println("Suspected [Stack overflow][$depth] in $h")
        }
        yield(h) //何もしない選択
        h.world.ownSide.hand.cards.forEach {
            it.use(h).forEach {
                useChecked(it, depth + 1).forEach { yield(it) }
            }
        }
    }
}

class eStartStep : Maneuver {
    override val efName: String = "CoreStep"
    override fun use(h: History): ParallelWorld = sequenceOf(h).map_world {
        tr(step = step + 1)
    }
}

class eCoreStep(val incCore: Int = 1) : Maneuver {
    override val efName: String = "CoreStep"
    override fun use(h: History): ParallelWorld = sequenceOf(h).map_world {
        if (step > 1) {
            val postSide = ownSide.mutation {
                reserve.core += Core(1)
            }
            tr(ownSide = postSide)
        } else {
            this
        }
    }
}

class eDrawStep(val nDraw: Int = 1) : Maneuver {
    override val efName: String = "DrawStep"
    override fun use(h: History): ParallelWorld = sequenceOf(h).flatMap_ownSide {
        opMoveCards(HAND, deck.cards.top(nDraw).onlyTakeOneCase("eDrawStep(${nDraw}) but card num. of deck is ${deck.cards.size}"))
    }
}


class eRefreshStep() : Maneuver {
    override val efName: String = "RefreshStep"
    override fun use(h: History): ParallelWorld = sequenceOf(h).map_ownSide {
        mutation {
            reserve.core += trashCore.core
            trashCore.core = Core(0)
        }
    }
}


fun main() {
    val c00 = SpiritCard(Category.SPIRITCARD, "c00", Color.R, 0, Sbl.R, Sbl.R * 0, setOf(), listOf(Card.LevelInfo(1, 1, 1000)))
    val c11 = SpiritCard(Category.SPIRITCARD, "c11", Color.R, 1, Sbl.R, Sbl.R * 1, setOf(), listOf(Card.LevelInfo(1, 1, 1000)))
    val c22 = SpiritCard(Category.SPIRITCARD, "c32", Color.R, 2, Sbl.R, Sbl.R * 2, setOf(), listOf(Card.LevelInfo(1, 1, 1000)))
    val c32 = SpiritCard(Category.SPIRITCARD, "c32", Color.R, 3, Sbl.R, Sbl.R * 2, setOf(), listOf(Card.LevelInfo(1, 1, 1000)))
    val c63 = SpiritCard(Category.SPIRITCARD, "c63", Color.R, 6, Sbl.R, Sbl.R * 3, setOf(), listOf(Card.LevelInfo(1, 1, 1000)))

    fun ParallelWorld.opドロー(n: Int): ParallelWorld = flatMap_ownSide { opMoveCards(HAND, deck.top(n)) }

    History.eden(setOf(c00, c11, c32), setOf())
            .assert { toList()[0].world.ownSide.reserve.core == Core(4, 1) }
            .flatMap_ownSide { opMoveCore(Core(1, 0), CORETRASH, RESERVE) } //普通のコア1個リザーブからトラッシュに移動したら、
            .apply { toList()[0].world.ownSide.reserve.core == Core(3, 1) } //残りは3コア(Sコア1個)

            .flatMap_ownSide { opMoveCore(CORETRASH, listOf(RESERVE), 1) } //さらにコア1個(Sコアかどうかは不問)を移動したら、
            .apply { map { it.world.ownSide.reserve.core }.toSet() == setOf(Core(2, 0), Core(2, 1)) } //残りは2コア (ソウルコアを含む場合とふくまない場合がある)

            .effect(ePayCost(1))//1コスト支払ったら、
            .apply { map { it.world.ownSide.reserve.core }.toSet() == setOf(Core(1, 0), Core(1, 1)) } //残りは1コア (ソウルコアの場合と普通コアの場合がある)

            .forEach { }

    History.eden(setOf(c00, c11, c32), setOf())
            .flatMap_ownSide { opMoveCore(Core(2, 1), CORETRASH, RESERVE) } //2コア(Sコア1)を除いておく(テストのため)
            .assert { onlyTakeOneCase().world.ownSide.reserve.core.c == 2 }

            .opドロー(1)
            .assert { onlyTakeOneCase().world.ownSide.hand.cardSet == setOf(c00) }
            .assert { onlyTakeOneCase().world.ownSide.deck.cardList == listOf(c11, c32) }
            .assert { onlyTakeOneCase().world.ownSide.attr(c00).place == HAND }

            .flatMap_ownSide { opCreateFO(c00) } //手札のカード(コスト0軽減0)をFOに
            .flatMap_ownSide { opMoveCore(Core(1), fo(c00), RESERVE) } //リザーブからコアを置く
            .assert { onlyTakeOneCase().world.ownSide.reserve.core.c == 1 } //リザーブ残り
            .assert { onlyTakeOneCase().world.ownSide.foAttr(c00).core.c == 1 } //スピリット上のコア

            .flatMap_ownSide { opDestruct(fo(c00)) } // c00を消滅
            .assert { onlyTakeOneCase().world.ownSide.reserve.core == Core(2) } //リザーブ残り
            .assert { onlyTakeOneCase().world.ownSide.trashCards.cardSet == setOf(c00) }

            .opドロー(1)
            // .flatMap { Ef召喚配置(c11).use(it) }  //コスト1軽減1のスピを召喚
            .flatMap_ownSide { opPayCost(c11) } // コストを支払い
            .flatMap_ownSide { opCreateFO(c11) } // カードをフィールドに配置
            .flatMap_ownSide { opMoveCore(fo(c11), payableCoreHolders, 1) } // [TODO]とりあえす1コアおいてみる

            .assert { toList()[0].world.ownSide.hand.cardSet == setOf<Card>() } //手札は0枚
            .assert { toList()[0].world.ownSide.trashCore.core == Core(3, 1) } // トラッシュは3
            .assert { toList()[0].world.ownSide.reserve.core == Core(0) } // リザーブは0

            .forEach { }

    History.eden(setOf(c00, c11, c22, c63), setOf())
            .opドロー(4)
            .flatMap { e召喚配置(c00).use(it) }.flatMap { e消滅チェック().use(it) }
            .flatMap { e召喚配置(c11).use(it) }.flatMap { e消滅チェック().use(it) }
            .flatMap { e召喚配置(c22).use(it) }.flatMap { e消滅チェック().use(it) }
            .flatMap { e召喚配置(c63).use(it) }.flatMap { e消滅チェック().use(it) }
            .distinctBy { it.world.ownSide }
            .assert {
                map { it.world.ownSide.fieldObjectsMap[FO(c63.name)]!!.core }.toSet() == setOf(Core(1, 0), Core(1, 1))
            }
//            .pln { "${stn.ownSide} $${stn.ownSide.shortHash()}" }

    History.eden(setOf(c00, c11, c22, c63), setOf())
            .effect(eDrawStep(1))
            .assertEach { world.ownSide.hand.cards == listOf(c00) }
            .effect(eMainStep())
            .assert { count() == 3 } // 召喚しない、する(Sコア置く)、する(Sコア置かない)

            .effect(eDrawStep(3))
            .effect(eMainStep())
            .filter { it.world.ownSide.fieldObjectsMap.containsKey(FO(c63.name)) } // c63を召喚するケースだけ抽出
            .distinct()
            .assert { count() == 2 } // する(Sコア置く)、する(Sコア置かない)
}



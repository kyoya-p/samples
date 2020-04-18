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


fun ParallelWorld.flatMap_world(op: Sequence<World>.() -> Sequence<World>): ParallelWorld = flatMap { tr -> sequenceOf(tr.stn).op().map { History(prevStn = tr.prevStn, stn = it) } }
fun ParallelWorld.flatMap_ownSide(d: UInt = 0u, op: Side.() -> Sequence<Side>): ParallelWorld = flatMap_world { map_ownSide { flatMap { it.op() } } }
fun Sequence<History>.map_ownSide(d: UInt = 0u, op: Side.() -> Side): Sequence<History> = flatMap_world { map_ownSide { map { it.op() } } }
fun Sequence<World>.map_ownSide(d: Int = 0, op: Sequence<Side>.() -> Sequence<Side>): Sequence<World> = flatMap { s -> sequenceOf(s.ownSide).op().map { s.tr(ownSide = it) } }


// 状態遷移
data class History(val prevStn: History?, val stn: World)
typealias ParallelWorld = Sequence<History>


fun eden(deck: Set<Card>, enemyDeck: Set<Card>) = sequenceOf(History(null, World(ownSide = initialSide(deck), enemySide = initialSide(enemyDeck), step = 1)))


// すべての効果を再帰的に実行
class eMainStep : Effect {
    override val efName: String = "MainStep"

    override fun use(h: History): ParallelWorld = sequence {
        yield(h) //何もしない選択
        h.stn.ownSide.hand.cards.forEach {
            it.use(h).forEach {
                this@eMainStep.use(it).forEach { yield(it) }
            }
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

    eden(setOf(c00, c11, c32), setOf())
            .assert { toList()[0].stn.ownSide.reserve.core == Core(4, 1) }
            .flatMap_ownSide { opMoveCore(CORETRASH, RESERVE, Core(1, 0)) } //普通のコア1個リザーブからトラッシュに移動したら、
            .apply { toList()[0].stn.ownSide.reserve.core == Core(3, 1) } //残りは3コア(Sコア1個)

            .flatMap_ownSide { opMoveCore(CORETRASH, listOf(RESERVE), 1) } //さらにコア1個(Sコアかどうかは不問)を移動したら、
            .apply { map { it.stn.ownSide.reserve.core }.toSet() == setOf(Core(2, 0), Core(2, 1)) } //残りは2コア (ソウルコアを含む場合とふくまない場合がある)

            .flatMap_ownSide { opPayCost(1) } //1コスト支払ったら、
            .apply { map { it.stn.ownSide.reserve.core }.toSet() == setOf(Core(1, 0), Core(1, 1)) } //残りは1コア (ソウルコアの場合と普通コアの場合がある)

            .forEach { }

    eden(setOf(c00, c11, c32), setOf())
            .flatMap_ownSide { opMoveCore(CORETRASH, RESERVE, Core(2, 1)) } //2コア(Sコア1)を除いておく(テストのため)
            .assert { onlyTakeOneCase().stn.ownSide.reserve.core.c == 2 }

            .opドロー(1)
            .assert { onlyTakeOneCase().stn.ownSide.hand.cardSet == setOf(c00) }
            .assert { onlyTakeOneCase().stn.ownSide.deck.cardList == listOf(c11, c32) }
            .assert { onlyTakeOneCase().stn.ownSide.attr(c00).place == HAND }

            .flatMap_ownSide { opCreateFO(c00) } //手札のカード(コスト0軽減0)をFOに
            .flatMap_ownSide { opMoveCore(fo(c00), RESERVE, Core(1)) } //リザーブからコアを置く
            .assert { onlyTakeOneCase().stn.ownSide.reserve.core.c == 1 } //リザーブ残り
            .assert { onlyTakeOneCase().stn.ownSide.foAttr(c00).core.c == 1 } //スピリット上のコア

            .flatMap_ownSide { opDestruct(fo(c00)) } // c00を消滅
            .assert { onlyTakeOneCase().stn.ownSide.reserve.core == Core(2) } //リザーブ残り
            .assert { onlyTakeOneCase().stn.ownSide.trashCards.cardSet == setOf(c00) }

            .opドロー(1)
            // .flatMap { Ef召喚配置(c11).use(it) }  //コスト1軽減1のスピを召喚
            .flatMap_ownSide { opPayCost(c11) } // コストを支払い
            .flatMap_ownSide { opCreateFO(c11) } // カードをフィールドに配置
            .flatMap_ownSide { opMoveCore(fo(c11), payableCoreHolders, 1) } // [TODO]とりあえす1コアおいてみる

            .assert { toList()[0].stn.ownSide.hand.cardSet == setOf<Card>() } //手札は0枚
            .assert { toList()[0].stn.ownSide.trashCore.core == Core(3, 1) } // トラッシュは3
            .assert { toList()[0].stn.ownSide.reserve.core == Core(0) } // リザーブは0

            .forEach { }

    eden(setOf(c00, c11, c22, c63), setOf())
            .opドロー(4)
            .flatMap { e召喚配置(c00).use(it) }.flatMap { e消滅チェック().use(it) }
            .flatMap { e召喚配置(c11).use(it) }.flatMap { e消滅チェック().use(it) }
            .flatMap { e召喚配置(c22).use(it) }.flatMap { e消滅チェック().use(it) }
            .flatMap { e召喚配置(c63).use(it) }.flatMap { e消滅チェック().use(it) }
            .distinctBy { it.stn.ownSide }
            .assert {
                map { it.stn.ownSide.fieldObjectsMap[FO(c63.name)]!!.core }.toSet() == setOf(Core(1, 0), Core(1, 1))
            }
            .pln { "${stn.ownSide} $${stn.ownSide.shortHash()}" }


    eden(setOf(c00, c11, c22, c63), setOf())

}



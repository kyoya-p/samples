package BSSim

class Situation(
        val ownSide: Side_XXX,
        val enemySide: Side_XXX = Side_XXX(),
        val step: Int = 1
) : Comparable<Situation> {
    constructor(prevStn: Situation
                , ownSide: Side_XXX = prevStn.ownSide
                , enemySide: Side_XXX = prevStn.enemySide
                , step: Int = prevStn.step
    ) : this(ownSide = ownSide, enemySide = enemySide, step = step)

    override fun hashCode(): Int {
        return ownSide.hashCode() * 3131 + enemySide.hashCode() * 31 + step
    }

    override fun compareTo(o: Situation): Int {
        ownSide.compareTo(o.ownSide).let { if (it != 0) return it }
        enemySide.compareTo(o.enemySide).let { if (it != 0) return it }
        return step - o.step
    }

    // -----New API ----------------------------------------------------------
    inline fun tr(ownSide: Side_XXX = this.ownSide
                  , enemySide: Side_XXX = this.enemySide
                  , step: Int = this.step) = Situation(ownSide = ownSide, enemySide = enemySide, step = step)

    inline fun trOwnSide(op: Side_XXX.() -> Side_XXX): Situation = tr(ownSide = ownSide.op())
    inline fun trOwnSideSq(op: Side_XXX.() -> Sequence<Side_XXX>): Sequence<Situation> = ownSide.op().map { tr(ownSide = it) }

    inline fun trEnemySide(op: Side_XXX.() -> Side_XXX): Situation = tr(enemySide = enemySide.op())
    inline fun trEnemySideSq(op: Side_XXX.() -> Sequence<Side_XXX>): Sequence<Situation> = enemySide.op().map { tr(ownSide = it) }


    // ----------------------------------------------------------

    // pickしたカードとコアから新たなFOを生成
    inline fun createFieldObjectByPicked(): Situation {
        val card = ownSide.picked_Cards
        val core = ownSide.pickedCore
        val s = ownSide.tr(
                pickedCards = listOf()
                , pickedCore = Core(0)
                , field = ownSide.field + BSO(id = "", cards = card, core = core)
        )
        return Situation(this, ownSide = s)
    }

    inline fun extinctionFO(fo: BSO): Sequence<Situation> {
        val rems = ownSide.field.filter { it !== fo }
        val postSide = ownSide.addReserveCore(fo.core)
                .replaceFO(Side_XXX.CARDTRASH) { bso -> BSO(bso, cards = bso.cards + fo.cards) }
                .tr(field = rems)
        return sequenceOf(Situation(this, ownSide = postSide))
    }

    override fun toString() = "St{${ownSide}}"

    companion object {
        fun test() {
            println("Situation1-----")
            //盤面初期設定
            val deck1 = listOf(ちょうちん(), オルリ(), ちょうちん(), オルリ(), ちょうちん())
            val mySide1 = Side_XXX(listOf<Card>())
            val mySide11 = mySide1.replaceBSOsBy {
                if (it === deck) it.tr(cards = deck1) else it
            }

            mySide11.deckCards.assert(deck1)

            val mySide12 = mySide11.run {
                val initCard = deckCards.pickTop(2).take1Case().first
                moveCardsBy(hand, initCard) { it + initCard }
            }
            mySide12.deckCards.assert(listOf(ちょうちん(), オルリ(), ちょうちん()))
            mySide12.handCards.assert(listOf(ちょうちん(), オルリ()))

            // 1枚召喚処理
            val board0 = Situation(ownSide = mySide12, enemySide = Side_XXX())
            val op手札0 = board0.ownSide.handCards[0]
            val tr初期状態 = sequenceOf(Transition(board0, board0))
            val tr手札0_召喚後 = tr初期状態.action(op手札0)
            tr手札0_召喚後.toList()[0].pln("After=")
            tr手札0_召喚後.toList()[0].stn.ownSide.handCards.assert(listOf(ちょうちん(), オルリ()))
            tr手札0_召喚後.toList()[0].stn.ownSide.field[0].cards.assert(listOf(ちょうちん()))

            val tr手札1_召喚後 = tr手札0_召喚後.flatMap {
                val h = it.stn.ownSide.handCards[0]
                it.action(op配置(h))
            }

            tr手札1_召喚後.toList().forEach { it.pln(".... ") }

            println("Situation2-----")
            val mySide3 = Side_XXX(deck1).run {
                opMoveCardToBtm(Side_XXX.DECK, Side_XXX.HAND, deckCards.pickTop(3).take1Case().first).take1Case()
            }

            val board3 = Situation(ownSide = mySide3)
            val tr3 = Transition(board3, board3)
            sequenceOf(tr3).action(op配置(deck1[0])).forEach {
                it.pln("YYYY: ")
            }


            println("Situation3-----")

            // 複数枚召喚処理
            val Lv111k = listOf(Card.LevelInfo(1, 1, 1000))

            class SP00() : SpiritCard(Category.SPIRITCARD, "SP00", Color.R, 0, Sbl.R, Sbl.Zero, setOf(), Lv111k)
            class SP11() : SpiritCard(Category.SPIRITCARD, "SP11", Color.R, 1, Sbl.R, Sbl.R * 1, setOf(), Lv111k)
            class SP22() : SpiritCard(Category.SPIRITCARD, "SP22", Color.R, 2, Sbl.R, Sbl.R * 2, setOf(), Lv111k)
            class SP63() : SpiritCard(Category.SPIRITCARD, "SP63", Color.R, 6, Sbl.R, Sbl.R * 3, setOf(), Lv111k)

            val deck01 = listOf(SP00(), SP11(), SP22(), SP63())
            val mySide01 = Side_XXX(deck01)
            val board1 = Situation(ownSide = mySide01)
            val initTr1 = sequenceOf(Transition(board1, board1)).sqOwnSide_flatMap {
                deckCards.pickTop(3).map { (pick, r) ->
                    //replaceCards(mapOf(deck to r, hand to hand.cards + pick))
                    moveCardsBy(deck, pick) { dk -> dk + pick }
                }
            }
            initTr1.toList()[0].stn.ownSide.deck.pln("hand=")
            initTr1.toList()[0].stn.ownSide.hand.pln("deck=")

        }


        /*
        res21.filter
        { it.ownSide.fieldSimbols.toInt() == 3 }.map
        { it.ownSide }.count().assert(2)// シンボル3個残るパターンは2つ
        res21.filter
        { it.ownSide.field.any { it.cards[0] == SP63() } }.map
        { it.ownSide }.count().assert(1)// コスト6スピが残るパターンは1つ
    */
    }

}

fun <T> Sequence<T>.take1Case(): T = take(1).toList()[0]

// Transitionシーケンス中にSituationシーケンスをはさむ
fun Sequence<Transition>.sqSituation(op: Sequence<Situation>.() -> Sequence<Situation>): Sequence<Transition> = flatMap { tr -> sequenceOf(tr.stn).op().map { Transition(prevStn = tr.prevStn, stn = it) } }
fun Sequence<Transition>.sqSituation_flatMap(d: UInt = 0u, op: Situation.() -> Sequence<Situation>): Sequence<Transition> = this.sqSituation { flatMap { it.op() } }
fun Sequence<Transition>.sqSituation_map(d: UInt = 0u, op: Situation.() -> Situation): Sequence<Transition> = this.sqSituation { map { it.op() } }

fun Sequence<Transition>.sqOwnSide(d: UInt = 0u, op: Sequence<Side_XXX>.() -> Sequence<Side_XXX>): Sequence<Transition> = this.sqSituation { sqOwnSide { op() } }
fun Sequence<Transition>.sqOwnSide_flatMap(d: UInt = 0u, op: Side_XXX.() -> Sequence<Side_XXX>): Sequence<Transition> = this.sqSituation { sqOwnSide { flatMap { it.op() } } }
fun Sequence<Transition>.sqOwnSide_map(d: UInt = 0u, op: Side_XXX.() -> Side_XXX): Sequence<Transition> = this.sqSituation { sqOwnSide { map { it.op() } } }

fun Sequence<Situation>.sqOwnSide(d: Int = 0, op: Sequence<Side_XXX>.() -> Sequence<Side_XXX>): Sequence<Situation> = flatMap { s -> sequenceOf(s.ownSide).op().map { s.tr(ownSide = it) } }

// 決まった個数(Sコアは不問)のコアを移動
inline fun Sequence<Situation>.opMoveCore(src: Int, dst: Int, pick: Int): Sequence<Situation> = flatMap { stn ->
    stn.ownSide.opMoveCore(src, dst, pick).map { stn.tr(ownSide = it) }
}


// 自分の手札から指定カード1枚をピックアップ(同じカードが複数枚ある場合、そのうち1枚が選択される)
// ピックアップ後のパターンを生成する
inline fun Sequence<Situation>.pickHandCard(ca: Card): Sequence<Situation> = flatMap { stn ->
    stn.ownSide.handCards.pick(ca).map { (pk, rmn) ->
        Situation(stn, ownSide = stn.ownSide.tr(pickedCards = listOf(pk), hand = rmn))
    }
}

inline fun Sequence<Situation>.moveCard(ca: Card, dst: Int): Sequence<Situation> = flatMap { stn ->
    stn.ownSide.opMoveCard(ca, dst).map { s -> stn.tr(ownSide = s) }
}

inline fun <T> List<T>.permutation() = (0..size).flatMap { permutation(it) }
fun <T> Iterable<T>.permutation(n: Int): List<List<T>> =
        if (n == 0) listOf<List<T>>(listOf<T>())
        else mapIndexed { i, ch ->
            (take(i) + drop(i + 1)).permutation(n - 1).map { listOf(ch) + it }
        }.flatten()

// 状態遷移(効果)
class Transition(val prevStn: Situation, val stn: Situation) : Comparable<Transition> {
    override fun toString() = "Tr{${prevStn}->${stn}}"
    override fun hashCode(): Int = prevStn.hashCode() * 31 + stn.hashCode()
    override fun compareTo(other: Transition): Int = stn.compareTo(other.stn).if0 { prevStn.compareTo(other.prevStn) }

    inline fun action(effectable: Effectable): Sequence<Transition> = effectable.effect(this)
}


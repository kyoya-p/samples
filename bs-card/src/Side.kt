package BSSim

//片方の盤面
//typealias Side = Set<Card>
//typealias MutableSide = MutableSet<Card>

data class Side(val cards: Map<Card, CardAttr> = mapOf(), val fos: Map<FO, FoAttr> = mapOf()) {
    override fun toString() = fos.values.toString()

    data class Mutable(val cards: MutableMap<Card, CardAttr.Mutable>, val fos: MutableMap<FO, FoAttr.Mutable>)
}


fun <K, V, MV> Map<K, V>.mapValueBy(op: (V) -> MV) = map { (k, v) -> k to op(v) }.toMap()
fun <K, V, MV> Map<K, V>.mMapValueBy(op: (V) -> MV) = map { (k, v) -> k to op(v) }.toMap().toMutableMap()

// Mutatation関連
fun Side.toMutableSide() = Side.Mutable(cards = cards.mMapValueBy { it.toMutable() }, fos = fos.map { (k, v) -> k to v.toMutable() }.toMap().toMutableMap())
fun Side.Mutable.toSide() = Side(cards = cards.mapValueBy { it.toImmutable() }, fos = fos.mapValueBy { it.toImmutable() })
inline fun Side.mutation(op: Side.Mutable.() -> Unit) = toMutableSide().apply { op() }.toSide()


val DECK = FO("Dk")
val HAND = FO("Hd")
val BURST = FO("Bu")
val CARDTRASH = FO("Tr")
val CORETRASH = FO("CT")
val LIFE = FO("Li")
val RESERVE = FO("CR")
val PICKEDCARD = FO("Pk")
val PICKEDCORE = FO("CP")

val Side.deck get() = this.fos[DECK]!!
val Side.hand get() = this.fos[HAND]!!
val Side.burst get() = this.fos[BURST]!!
val Side.life get() = this.fos[LIFE]!!
val Side.trashCards get() = this.fos[CARDTRASH]!!
val Side.trashCore get() = this.fos[CORETRASH]!!
val Side.reserve get() = this.fos[RESERVE]!!
val Side.pickedCards get() = this.fos[PICKEDCARD]!!
val Side.pickedCore get() = this.fos[PICKEDCORE]!!

var Side.Mutable.reserve
    get() = fos[RESERVE]!!
    set(v) {
        fos[RESERVE] = v
    }
var Side.Mutable.trashCore
    get() = fos[CORETRASH]!!
    set(v) {
        fos[CORETRASH] = v
    }


fun initialSide(deck: Set<Card>): Side {
    val places = mutableMapOf<FO, FoAttr>()
    val cards = mutableMapOf<Card, CardAttr>()

    places[DECK] = FoAttr(id = DECK, cardOrdering = true)
    places[HAND] = FoAttr(id = HAND)
    places[CARDTRASH] = FoAttr(id = CARDTRASH)
    places[CORETRASH] = FoAttr(id = CORETRASH)
    places[LIFE] = FoAttr(id = LIFE, core = Core(5))
    places[RESERVE] = FoAttr(id = RESERVE, core = Core(4, 1))
    places[PICKEDCARD] = FoAttr(id = PICKEDCARD)
    places[PICKEDCORE] = FoAttr(id = PICKEDCORE)

    deck.forEach {
        cards[it] = CardAttr(DECK)
        places[DECK] = places[DECK]!!.stackBottom(listOf(it))
    }
    return Side(cards, places)
}

fun Side.tr(cards: Map<Card, CardAttr> = this.cards, places: Map<FO, FoAttr> = this.fos): Side = Side(cards, places)
fun Side.attr(p: FO): FoAttr = fos[p]!!
fun Side.attr(c: Card): CardAttr = cards[c]!!
fun Side.fo(c: Card): FO = attr(c).place
fun Side.foAttr(c: Card): FoAttr = attr(attr(c).place)

// ----------------------------------------------------------------
// 基本操作(整合性保証しない)

fun Side.opCreateFO(card: Card, cardOrdering: Boolean = true): Sequence<Side> = sequenceOf(
        mutation {
            val preFo = cards[card]!!.place
            val newFo = FieldFO(card.name)
            val newFoAttr = FoAttr(id = newFo, core = Core(0), cardOrdering = cardOrdering, cards = listOf(card))

            fos[preFo]!!.cards.remove(card)
            fos.put(newFo, newFoAttr.toMutable())
            cards[card]!!.place = newFo
        }
)

fun Side.deleteFO(fo: FO): Side = mutation {
    if (attr(fo).cardSet.isNotEmpty() || attr(fo).core != Core(0)) Exception("This FO is still alive!")
    fos.remove(fo)
}


fun Side.setPlaceCardBy(tg: FO, op: (Cards) -> Cards): Side = mutation {
    fos[tg] = attr(tg).tr(cards = op(attr(tg).cards)).toMutable()
}

fun Side.removeFO(fo: FO): Side = mutation { fos.remove(fo) }

// ----------------------------------------------------------------
// BS固有の設定
// フィールドオブジェクト以外の全オブジェクト(カード/コアを置ける場所)
val Side.places: List<FO>
    get() = listOf(
            DECK,
            HAND,
            BURST,
            LIFE,
            CARDTRASH,
            CORETRASH,
            RESERVE,
            PICKEDCARD,
            PICKEDCORE
    ) + fieldObjects

val Side.fieldObjects: List<FO> get() = fos.keys.filter { it is FieldFO }
val Side.fieldObjectsMap: Map<FO, FoAttr> get() = fos.filter { it.key is FieldFO }

// ----------------------------------------------------------------
// コア関係
val Side.payableCoreHolders: List<FO> get() = listOf(RESERVE) + fieldObjects


fun Side.putCoreBy(dst: FO, op: (Core) -> Core): Side = mutation {
    fos[dst]!!.core = op(fos[dst]!!.core)
}


// コア取り出し可能なBSOから特定BSOへのコアの移動
fun Side.opMoveCore(dst: FO, srcFos: List<FO>, core: Int): Sequence<Side> {
    val h = srcFos.map { fos[it]!!.core }
    return pickCore(core, h).map { (picked, rem) ->
        mutation {
            rem.forEachIndexed { i, c -> fos[srcFos[i]]!!.core = c } //抜きだしたコア状態を反映し
            fos[dst]!!.core += picked  //取り出したコアを対象に置く
        }
    }
}

fun Side.opMoveCore(core: Core, dst: FO, src: FO): Sequence<Side> = opMoveCore(core, dst, listOf(src))
fun Side.opMoveCore(core: Core, dst: FO, srcFos: List<FO>): Sequence<Side> {
    val h = srcFos.map { fos[it]!!.core }
    return pickCore(core, h).map { (picked, rem) ->
        mutation {
            rem.forEachIndexed { i, c -> fos[srcFos[i]]!!.core = c } //抜きだしたコア状態を反映し
            fos[dst]!!.core += picked  //取り出したコアを対象に置く
        }
    }
}

class eMoveCore(val core: Int, val dst: FO, val srcFos: List<FO>) : Effect {
    override val efName = "コア移動"
    override fun use(h: History): ParallelWorld = sequenceOf(h)
            .flatMap_ownSide {
                opMoveCore(dst, srcFos, core)
            }
}

fun Side.opPayCost(cost: Int): Sequence<Side> = opMoveCore(CORETRASH, payableCoreHolders, cost)

fun Side.opPayCost(card: Card): Sequence<Side> {
    fun Int.min0() = if (this < 0) 0 else this
    val reqCore = (card.cost - fieldSimbols.reduction(card.reduction)).min0()
    return opPayCost(reqCore)
}

// ----------------------------------------------------------------
// カード関係

fun Side.opMoveCardsBy(dst: FO, cs: Cards, opInsert: (dst: Cards, cs: Cards) -> Sequence<Cards>): Sequence<Side> =
        opInsert(attr(dst).cards, cs).map { dstCards ->
            mutation {
                for (c in cs) {
                    val src = cards[c]!!.place
                    fos[src]!!.cards.remove(c)
                    cards[c]!!.place = dst
                }
                fos[dst]!!.cards = dstCards.toMutableList()
            }
        }

fun Side.opMoveCards(dst: FO, cs: Cards): Sequence<Side> =
        opMoveCardsBy(dst, cs) { dst, cs -> sequenceOf(dst + cs) }

fun Side.opMoveCard(dst: FO, c: Card): Sequence<Side> =
        opMoveCardsBy(dst, listOf(c)) { dst, cs -> sequenceOf(dst + cs) }


// ----------------------------------------------------------------
// 複合操作
inline val Side.fieldSimbols get() = fieldObjects.map { attr(it).cards[0].simbols }.fold(Sbl()) { s, sbl -> s + sbl }


// FO消滅(維持コア以下にする(維持コア0のFOは消滅できない))
fun Side.opDestruct(fo: FO): Sequence<Side> = sequenceOf(this)
        .filter { attr(fo).cards[0].lvInfo[0].keepCore >= 1 } //維持コア1以上
        .flatMap { opMoveCore(RESERVE, listOf(fo), attr(fo).core.c) } //コアをリザーブに移動
        .flatMap { it.opMoveCards(CARDTRASH, attr(fo).cards) } //FOのカードをすべてトラッシュへ移動
        .map { it.deleteFO(fo) } //空FOを削除

// ----------------------------------------------------------------
// シミュレーション補助

// Bottomまでの枚数(初期設定でBottomカードを仕込んでおくこと)
fun Side.deckDepth(bottomMark: Card): Int = deck.cards.lastIndexOf(bottomMark)

// ----------------------------------------------------------------
// Test

fun main() {
    {
        //pretest
        val a123 = listOf(1, 2, 3)
        val a321 = listOf(3, 2, 1)
        a123 assertNot a321
        a123.toSet() assert a321.toSet()
        a123.toHashSet() assert a321.toHashSet()

        setOf(1, 2, 3) assert setOf(3, 2, 1)
        setOf(1, 2, 3).toList() assertNot setOf(3, 2, 1).toList()
    }()

    class Card1 : SpiritCard(Category.SPIRITCARD, "C1", Color.Y, 0, Sbl.Y, Sbl.Zero, setOf(), listOf())
    class Card2 : SpiritCard(Category.SPIRITCARD, "C2", Color.Y, 0, Sbl.Y, Sbl.Zero, setOf(), listOf())
    class Card3 : SpiritCard(Category.SPIRITCARD, "C3", Color.Y, 0, Sbl.Y, Sbl.Zero, setOf(), listOf())

    val C1 = Card1()
    val C2 = Card2()
    val C3 = Card3()

    val s123 = initialSide(setOf(C1, C2, C3))
    val s123b = initialSide(setOf(C1, C2, C3))
    val s213 = initialSide(setOf(C2, C1, C3))

    s123.deck.cards assert listOf(C1, C2, C3)
    s123.deck.cards assertNot listOf(C1, C3, C2)

    s123.cards assert s123b.cards
    s123.fos assert s123b.fos
    s123 assert s123b

    s123.cards assert s213.cards
    s123.fos assertNot s213.fos
    s123 assertNot s213

    // トップ2枚の順番が違っていても2ドローで同じ状態になる
    val s123s2 = s123.opMoveCards(HAND, s123.deck.top(2)).onlyTakeOneCase()
    val s213s2 = s213.opMoveCards(HAND, s213.deck.top(2)).onlyTakeOneCase()

    s123s2.deck.cards assert listOf(C3)
    s123s2.hand.cards assert listOf(C1, C2)

    s213s2.deck.cards assert listOf(C3)
    s213s2.hand.cards assert listOf(C2, C1)
    s213s2.hand.cards assert setOf(C2, C1).toList()

    s123s2.fos[HAND]!!.cards assertNot s213s2.fos[HAND]!!.cards
    s123s2.fos[HAND] assert s213s2.fos[HAND]
    s123s2.fos assert s213s2.fos
    s123s2 assert s213s2

}


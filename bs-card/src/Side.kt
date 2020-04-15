package BSSim

//片方の盤面
//typealias Side = Set<Card>
//typealias MutableSide = MutableSet<Card>

data class Side(val cards: Map<Card, CardAttr> = mapOf(), val fos: Map<FO, FoAttr> = mapOf()) {
    override fun toString() = fos.values.toString()
}

data class MutableSide(val cards: MutableMap<Card, CardAttr>, val places: MutableMap<FO, FoAttr>)


// Mutatation関連
fun Side.toMutableSide() = MutableSide(cards.toMutableMap(), fos.toMutableMap())
fun MutableSide.toMutableSide() = this
fun MutableSide.toSide() = Side(cards, places)
fun Side.toSide() = this
inline fun Side.mutation(op: MutableSide.() -> Unit) = toMutableSide().apply { op() }.toSide()
inline fun MutableSide.mutation(op: MutableSide.() -> Unit) = apply { op() }

fun MutableSide.upd(fo: FO, op: (FoAttr) -> FoAttr) {
    places[fo] = op(places[fo]!!)
}

fun MutableSide.upd(c: Card, op: (CardAttr) -> CardAttr) {
    cards[c] = op(cards[c]!!)
}

fun MutableSide.muMoveCardsBy(dst: FO, cs: Cards, opInsert: (Cards) -> Cards) {
    for (c in cs) {
        val src = cards[c]!!.place
        upd(c) { it.tr(place = dst) }
        upd(src) { it.tr(cards = it.cards.remove(c)) }
    }
    upd(dst) {
        it.tr(cards = opInsert(it.cards))
    }
}

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
fun Side.appendPlace(fo: FO, attr: FoAttr): Side = mutation {
    places.put(fo, attr)
}

fun Side.deletePlace(fo: FO): Side = mutation {
    if (attr(fo).cardSet.isNotEmpty() || attr(fo).core != Core(0)) Exception("This FO is still alive!")
    places.remove(fo)
}

fun Side.setCardPlaceBy(tg: Card, op: (FO) -> FO): Side = mutation {
    cards[tg] = attr(tg).tr(place = op(attr(tg).place))
}

fun Side.setPlaceCardBy(tg: FO, op: (Cards) -> Cards): Side = mutation {
    places[tg] = attr(tg).tr(cards = op(attr(tg).cards))
}

fun Side.opCreateNewFO(newFo: FO, cardOrdering: Boolean = false): Sequence<Side> = sequenceOf(
        mutation {
            val newFoAttr = FoAttr(newFo, core = Core(0), cardOrdering = cardOrdering, cards = listOf<Card>())
            places[newFo] = newFoAttr
        }
)

fun Side.opCreateNewFO(card: Card, cardOrdering: Boolean = true): Sequence<Side> = sequenceOf(
        mutation {
            val preFo = cards[card]!!.place
            val newFo = FO(card.name)
            val newFoAttr = FoAttr(id = newFo, core = Core(0), cardOrdering = cardOrdering, cards = listOf(card))

            places[preFo] = places[preFo]!!.tr { cards.remove(card) }
            places.put(newFo, newFoAttr)
            cards[card] = cards[card]!!.tr { place = newFo }
        }
)

fun Side.removeFO(fo: FO): Side = mutation { places.remove(fo) }

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

// ----------------------------------------------------------------
// コア関係
val Side.payableCoreHolders: List<FO> get() = listOf(RESERVE) + fieldObjects

fun Side.putCoreBy(dst: FO, op: (Core) -> Core): Side {
    val pre = fos[dst]!!.core
    val post = op(pre)
    return tr(places = fos.toMutableMap().also { it[dst] = it[dst]!!.tr(core = post) })
}

// srcからdstへコアを移動
fun Side.opMoveCore(dst: FO, src: FO, core: Core): Sequence<Side> = sequence {
    if (fos[src]!!.core.contains(core)) { //srcに移動するだけのコアがあるなら
        yield(putCoreBy(src) { it - core }.putCoreBy(dst) { it + core })
    } else {
        println("No movable cores.")
    }
}

// コア取り出し可能なBSOから特定BSOへのコアの移動
fun Side.opMoveCore(dst: FO, srcs: List<FO>, core: Int): Sequence<Side> = sequence {
    srcs.map { fos[it]!!.core }.pickCore(core).forEach { (picked, rem) ->
        val m = srcs.zip(rem).associate { (p, c) -> p to c }
        val post = mutation {

            //ピックアップ対象のコアホルダからコア取り上げ
            m.forEach { (fo, c) ->
                places[fo] = places[fo]!!.tr(core = c)

            }
            // places.forEach { places[it.key] = places[it.key]!!.tr(core = m[it.key]!!) }
            putCoreBy(dst) { it + picked } //取り出したコアを対象に置く
        }
        yield(post)
    }
}

// ----------------------------------------------------------------
// カード関係

// 指定のFOにカードを移動する
fun Side.moveCardBy(dst: FO, c: Card, opInsert: (dst: Cards, c: Card) -> Cards): Side = this
        .setPlaceCardBy(dst) { opInsert(it, c) }
        .setCardPlaceBy(c) { dst }
        .setPlaceCardBy(attr(c).place) { it.remove(c) }

fun Side.opMoveCardsBy(dst: FO, cs: Cards, opInsert: (dst: Cards, cs: Cards) -> Sequence<Cards>): Sequence<Side> = sequence {
    opInsert(attr(dst).cards, cs).forEach { tgCards ->
        val s1 = mutation {
            muMoveCardsBy(dst, cs) { cs + it }
        }
        yield(s1)
    }
}

fun Side.opMoveCards(dst: FO, cs: Cards): Sequence<Side> =
        opMoveCardsBy(dst, cs) { dst, cs -> sequenceOf(dst + cs) }

fun Side.opMoveCard(dst: FO, c: Card): Sequence<Side> = sequenceOf(
        setCardPlaceBy(c) { dst }
                .setPlaceCardBy(dst) { it + listOf(c) }
                .setPlaceCardBy(attr(c).place) { it.remove(c) }
)

// ----------------------------------------------------------------
// 複合操作
inline val Side.fieldSimbols get() = fieldObjects.map { attr(it).cards[0].simbols }.fold(Sbl()) { s, sbl -> s + sbl }

fun Side.opPayCost(cost: Int): Sequence<Side> = opMoveCore(CORETRASH, payableCoreHolders, cost)

// FO消滅(維持コア以下にする(維持コア0のFOは消滅できない))
fun Side.opDestruct(fo: FO): Sequence<Side> = sequence {
    if (attr(fo).cards[0].lvInfo[0].keepCore >= 1) { //維持コア1以上
        opMoveCore(RESERVE, fo, attr(fo).core) //コアをリザーブに移動
                .flatMap { opMoveCards(CARDTRASH, attr(fo).cards) } //FOのカードをすべてトラッシュへ移動
                .map { deletePlace(fo) } //空っぽのFOアクセス不能
                .forEach {
                    yield(it)
                }
    }
}


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

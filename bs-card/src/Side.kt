package BSSim

//片方の盤面
//typealias Side = Set<Card>
//typealias MutableSide = MutableSet<Card>

data class Side(val cards: Map<Card, CardAttr> = mapOf(), val places: Map<Place, PlaceAttr> = mapOf())
data class MutableSide(val cards: MutableMap<Card, CardAttr>, val places: MutableMap<Place, PlaceAttr>)

// Mutatation関連
fun Side.toMutableSide() = MutableSide(cards.toMutableMap(), places.toMutableMap())
fun MutableSide.toMutableSide() = this
fun MutableSide.toSide() = Side(cards, places)
fun Side.toSide() = this
fun Side.mutation(op: MutableSide.() -> Unit) = toMutableSide().apply { op() }.toSide()
fun MutableSide.mutation(op: MutableSide.() -> Unit) = apply { op() }

fun MutableSide.upd(fo: Place, op: (PlaceAttr) -> PlaceAttr) {
    places[fo] = op(places[fo]!!)
}

fun MutableSide.upd(c: Card, op: (CardAttr) -> CardAttr) {
    cards[c] = op(cards[c]!!)
}

fun MutableSide.muMoveCardsBy(dst: Place, cs: Cards, opInsert: (Cards) -> Cards) {
    for (c in cs) {
        val src = cards[c]!!.place
        upd(c) { it.tr(place = dst) }
        upd(src) { it.tr(cards = it.cards.remove(c)) }
    }
    upd(dst) {
        it.tr(cards = opInsert(it.cards))
    }
}

val DECK = Place("Dk")
val HAND = Place("Hd")
val BURST = Place("Bu")
val CARDTRASH = Place("Tr")
val CORETRASH = Place("CT")
val LIFE = Place("Li")
val RESERVE = Place("CR")
val PICKEDCARD = Place("Pk")
val PICKEDCORE = Place("CP")

val Side.deck get() = this.places[DECK]!!
val Side.hand get() = this.places[HAND]!!
val Side.burst get() = this.places[BURST]!!
val Side.life get() = this.places[LIFE]!!
val Side.trashCards get() = this.places[CARDTRASH]!!
val Side.trashCore get() = this.places[CORETRASH]!!
val Side.reserve get() = this.places[RESERVE]!!
val Side.pickedCards get() = this.places[PICKEDCARD]!!
val Side.pickedCore get() = this.places[PICKEDCORE]!!

fun initialSide(deck: List<Card>): Side {
    val places = mutableMapOf<Place, PlaceAttr>()
    val cards = mutableMapOf<Card, CardAttr>()

    places[DECK] = PlaceAttr(cardOrdering = true)
    places[HAND] = PlaceAttr()
    places[CARDTRASH] = PlaceAttr()
    places[CORETRASH] = PlaceAttr()
    places[LIFE] = PlaceAttr(core = Core(5))
    places[RESERVE] = PlaceAttr(core = Core(4, 1))
    places[PICKEDCARD] = PlaceAttr()
    places[PICKEDCORE] = PlaceAttr()

    deck.forEach {
        cards[it] = CardAttr(DECK)
        places[DECK] = places[DECK]!!.stackBottom(listOf(it))
    }
    return Side(cards, places)
}

fun Side.tr(cards: Map<Card, CardAttr> = this.cards, places: Map<Place, PlaceAttr> = this.places): Side = Side(cards, places)
fun Side.attr(p: Place): PlaceAttr = places[p]!!
fun Side.attr(c: Card): CardAttr = cards[c]!!


// ----------------------------------------------------------------
// 基本操作(整合性保証しない)
fun Side.appendPlace(fo: Place, attr: PlaceAttr): Side = mutation {
    places.put(fo, attr)
}

fun Side.deletePlace(fo: Place): Side = mutation {
    if (attr(fo).cardSet.isNotEmpty() || attr(fo).core != Core(0)) Exception("This FO is still alive!")
    places.remove(fo)
}

fun Side.setCardPlaceBy(tg: Card, op: (Place) -> Place): Side = mutation {
    cards[tg] = attr(tg).tr(place = op(attr(tg).place))
}

fun Side.setPlaceCardBy(tg: Place, op: (Cards) -> Cards): Side = mutation {
    places[tg] = attr(tg).tr(cards = op(attr(tg).cards))
}


// ----------------------------------------------------------------
// BS固有の設定
// フィールドオブジェクト以外の全オブジェクト(カード/コアを置ける場所)
val Side.places: List<Place>
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

val Side.fieldObjects: List<Place> get() = places.keys.filter { it is FieldPlace }

// ----------------------------------------------------------------
// コア関係
val Side.payableCoreHolders: List<Place> get() = listOf(RESERVE) + fieldObjects

fun Side.putCoreBy(dst: Place, op: (Core) -> Core): Side {
    val pre = places[dst]!!.core
    val post = op(pre)
    return tr(places = places.toMutableMap().also { it[dst] = it[dst]!!.tr(core = post) })
}

// srcからdstへコアを移動
fun Side.opMoveCore(dst: Place, src: Place, core: Core): Sequence<Side> = sequence {
    if (places[src]!!.core.contains(core)) { //srcに移動するだけのコアがあるなら
        yield(putCoreBy(src) { it - core }.putCoreBy(dst) { it + core })
    }
}

// コア取り出し可能なBSOから特定BSOへのコアの移動
inline fun Side.opMoveCore(dst: Place, srcs: List<Place>, core: Int): Sequence<Side> = sequence {
    srcs.map { places[it]!!.core }.pickCore(core).forEach { (picked, rem) ->
        val m = srcs.zip(rem).associate { (p, c) -> p to c }
        val post = mutation {
            places.forEach {
                places[it.key] = places[it.key]!!.tr(core = m[it.key]!!)
            }
            putCoreBy(dst) { it + picked }
        }
        yield(post)
    }
}

// ----------------------------------------------------------------
// カード関係

// 指定のFOにカードを移動する
fun Side.moveCardBy(dst: Place, c: Card, opInsert: (dst: Cards, c: Card) -> Cards): Side = this
        .setPlaceCardBy(dst) { opInsert(it, c) }
        .setCardPlaceBy(c) { dst }
        .setPlaceCardBy(attr(c).place) { it.remove(c) }

fun Side.opMoveCardsBy(dst: Place, cs: Cards, opInsert: (dst: Cards, cs: Cards) -> Sequence<Cards>): Sequence<Side> = sequence {
    opInsert(attr(dst).cards, cs).forEach { tgCards ->
        val s1 = mutation {
            muMoveCardsBy(dst, cs) { cs + it }
        }
        yield(s1)
    }
}

inline fun Side.opMoveCards(dst: Place, cs: Cards): Sequence<Side> =
        opMoveCardsBy(dst, cs) { dst, cs -> sequenceOf(dst + cs) }

inline fun Side.opMoveCard(dst: Place, c: Card): Sequence<Side> = sequenceOf(
        setCardPlaceBy(c) { dst }
                .setPlaceCardBy(dst) { it + listOf(c) }
                .setPlaceCardBy(attr(c).place) { it.remove(c) }
)

// ----------------------------------------------------------------
// 複合操作
inline val Side.fieldSimbols get() = fieldObjects.map { attr(it).cards[0].simbols }.fold(Sbl()) { s, sbl -> s + sbl }

inline fun Side.opPayCost(cost: Int): Sequence<Side> = opMoveCore(CORETRASH, payableCoreHolders, cost)

// FO消滅(維持コア以下にする(維持コア0のFOは消滅できない))
inline fun Side.opDestruct(fo: Place): Sequence<Side> = sequence {
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

    val s123 = initialSide(listOf(C1, C2, C3))
    val s123b = initialSide(listOf(C1, C2, C3))
    val s213 = initialSide(listOf(C2, C1, C3))

    s123.deck.cards assert listOf(C1, C2, C3)
    s123.deck.cards assertNot listOf(C1, C3, C2)

    s123.cards assert s123b.cards
    s123.places assert s123b.places
    s123 assert s123b

    s123.cards assert s213.cards
    s123.places assertNot s213.places
    s123 assertNot s213

    // トップ2枚の順番が違っていても2ドローで同じ状態になる
    val s123s2 = s123.opMoveCards(HAND, s123.deck.top(2)).onlyTakeOneCase()
    val s213s2 = s213.opMoveCards(HAND, s213.deck.top(2)).onlyTakeOneCase()

    s123s2.deck.cards assert listOf(C3)
    s123s2.hand.cards assert listOf(C1, C2)

    s213s2.deck.cards assert listOf(C3)
    s213s2.hand.cards assert listOf(C2, C1)
    s213s2.hand.cards assert setOf(C2, C1).toList()

    s123s2.places[HAND]!!.cards assertNot s213s2.places[HAND]!!.cards
    s123s2.places[HAND] assert s213s2.places[HAND]
    s123s2.places assert s213s2.places
    s123s2 assert s213s2

}

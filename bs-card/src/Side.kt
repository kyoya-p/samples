package BSSim

import java.util.*

inline fun <T> List<T>.pickTop(n: Int): Sequence<Pair<List<T>, List<T>>> = if (n <= size) sequenceOf(take(n) to drop(n)) else sequenceOf() //1枚ずつ取り出し、最初に取り出したものが[0]
inline fun <T> List<T>.pickBottom(n: Int): Sequence<Pair<List<T>, List<T>>> = if (n <= size) sequenceOf(drop(n).reversed() to take(n)) else sequenceOf() //1枚ずつ取り出し、最初に取り出したものが[0]
inline fun <T> List<T>.putTop(i: List<T>): List<T> = i.reversed() + this //[0]を最初として順に1枚ずつ置く。
inline fun <T> List<T>.putBottom(i: List<T>): List<T> = this + i //[0]を最初として順に1枚ずつ置く。


typealias  BSOmap = SortedMap<Card, BSO>
typealias  BSOlist = Map<Card, BSO>

fun BSOmap.hashCode() = entries.fold(0) { s, t -> s + (t.key.hashCode() * 31) + t.value.hashCode() }
fun BSOlist.hashCode() = entries.fold(0) { s, t -> s * 97 + (t.key.hashCode() * 31) + t.value.hashCode() }

//片方の盤面
class Side(
        val deck: Cards = Cards()
        , val hand: Cards = Cards()
        , val burst: Cards = Cards()
        , val pickedCards: Cards = Cards()
        , val trashCards: Cards = Cards()

        , val life: Core = Core(5, 0)
        , val pickedCore: Core = Core(0)
        , val trashCore: Core = Core(0)
        , val reserve: Core = Core(4, 0)

//        val bso: BSOlist, // デッキ、手札、等
        , val fo: BSOmap //スピリットなど、フィールド上のモノ
) : Comparable<Side> {

    companion object {
        val PICKEDCARD = BSO.Companion.VCard(Category.PICKEDCARD)
        val PICKEDCORE = BSO.Companion.VCard(Category.PICKEDCORE)
        val DECK = BSO.Companion.VCard(Category.DECK)
        val HAND = BSO.Companion.VCard(Category.HAND)
        val BURST = BSO.Companion.VCard(Category.BURST)
        val CARDTRASH = BSO.Companion.VCard(Category.CARDTRUSH)
        val CORETRASH = BSO.Companion.VCard(Category.CORETRASH)
        val LIFE = BSO.Companion.VCard(Category.LIFE)
        val RESERVE = BSO.Companion.VCard(Category.RESERVE)
    }

    override fun hashCode(): Int = cardHolder.fold(0) { s, t -> s * 31 + t.hashCode() } * 31 + coreHolder.fold(0) { s, t -> s * 31 + t.hashCode() }

    override fun equals(o: Any?) = compareTo(o as Side) == 0
    override fun compareTo(o: Side): Int {
        fun <T : Comparable<T>> Iterable<T>.compareTo(o: Iterable<T>): Int {
            val i1 = iterator()
            val i2 = o.iterator()
            while (i1.hasNext() || i2.hasNext()) {
                i1.hasNext().compareTo(i2.hasNext()).let { if (it != 0) return it }
                i1.next().compareTo(i2.next()).let { if (it != 0) return it }
            }
            return 0
        }
        return cardHolder.toList().compareTo(o.cardHolder.toList())
    }

    inline fun tr(
            deck: Cards = this.deck
            , hand: Cards = this.hand
            , burst: Cards = this.burst
            , trashCards: Cards = this.trashCards
            , trashCore: Core = this.trashCore
            , life: Core = this.life
            , reserve: Core = this.reserve
            , pickedCards: Cards = this.pickedCards
            , pickedCore: Core = this.pickedCore

            , fo: BSOmap = this.field
    ) = Side(
            deck = deck
            , hand = hand
            , burst = burst
            , life = life
            , trashCards = trashCards
            , trashCore = trashCore
            , reserve = reserve
            , pickedCards = pickedCards
            , pickedCore = pickedCore

            , fo = fo
    )

    val coreHolder: Sequence<Core> = sequence {
        yield(life)
        yield(trashCore)
        yield(reserve)
        yield(pickedCore)
    }

    val cardHolder: Sequence<Cards> = sequence {
        yield(deck)
        yield(hand)
        yield(burst)
        yield(trashCards)
        yield(pickedCards)
    }


    //inline val field: BSOmap get() = fo

    val payableCoreHolder: Sequence<Core> = sequence {
        yield(reserve)
        fo.forEach { yield(it.value.core) }
    }


    inline fun replaceFOBy(card: Card, op: (BSO) -> BSO): Side = tr(fo = fo.map { if (it.key == card) card to op() else card to it.value }.toMap().toSortedMap())
    inline fun replaceFO(card: Card, bso: BSO): Side = replaceFOBy(card) { bso }

//    inline fun replaceBSOsBy(op: Side.(BSO) -> BSO) = Side(bsObjects.map { t -> this.op(t) })
//    inline fun replaceBSOsBy(dst: BSO, op: (BSO) -> BSO) = tr(bsos = bsObjects.map { t -> if (t === dst) op(t) else t })
//    inline fun replaceBSOsBy(bsosDst: BSOs, op: (BSO) -> BSO) = tr(bsos = bsObjects.map { t -> if (bsosDst.contains(t)) op(t) else t })
//    inline fun replaceBSOs(m: Map<BSO, BSO>): Side_XXX = tr(bsObjects.map { m[it] ?: it })

    inline fun replaceCoreBy(dst: BSO, op: (Core) -> Core) = replaceBSOsBy(dst) { bso -> bso.tr(core = op(bso.core)) }

    inline fun moveCardsBy(dst: BSO, cs: Cards, op: (Cards) -> Cards): Side_XXX =
            replaceBSOsBy { o ->
                if (o === dst) {
                    o.tr(cards = op(o.cards))
                } else if (o.cards.intersect(cs).size == 0) {
                    o
                } else {
                    o.tr(cards = o.cards.drops(cs))
                }
            }

    // コア取り出し可能なBSOからコアの移動
    inline fun opMoveCore(bso: BSO, cost: Int): Sequence<Side_XXX> = sequence {
        payableCoreHolder.pickCore(cost).forEach { (picked, rem) -> // コスト支払い可能なBSOからコアを抽出
            yield(replacePayableCores(rem).replaceCoreBy(bso) { it + picked }) // 抽出したコアを指定BSOに置く
        }
    }


    // コストを支払う
    inline fun opPayCost(cost: Int): Sequence<Side_XXX> = opMoveCore(trashCore, cost) // コア取り出し可能なBSOからコアを集めてトラッシュに移動

    // ------------- Old API

    inline fun replaceFoBy(op: (BSOs) -> BSOs) = this.tr(bsos = op(bsObjects))
    inline fun setFieldObjects(fos: List<BSO>) = this.tr(bsos = fos)
    inline fun replaceFOs(op: (Int, BSO) -> BSO) = this.tr(bsos = bsObjects.mapIndexed { i, t -> op(i, t) })
    inline fun replaceFOIndexed(index: Int, op: (Int, BSO) -> BSO) = replaceFOs { i, t -> if (i == index) op(i, t) else t }
    inline fun replaceFO(index: Int, op: (BSO) -> BSO) = replaceFOs { i, t -> if (i == index) op(t) else t }
    inline fun replaceFO(bso: BSO, op: (BSO) -> BSO) = replaceFOs { i, t -> if (t === bso) op(t) else t }
    inline fun replaceFOCards(i: Int, op: (Cards) -> Cards) = replaceFO(i) { BSO(it, cards = op(it.cards)) }
    inline fun replaceFOCore(i: Int, op: (Core) -> Core) = replaceFO(i) { BSO(it, core = op(it.core)) }

    inline fun createFieldObject(newObject: BSO) = setFieldObjects(bsObjects + listOf(newObject))
    inline fun createFieldObject(cards: List<Card>, core: Core, id: String) = setFieldObjects(bsObjects + listOf(BSO(id = id, cards = cards, core = core)))
    inline fun delFieldObject(index: Int) = setFieldObjects(bsObjects.filterIndexed { i, _ -> i != index })
    inline fun delFieldObject(target: BSO) = setFieldObjects(bsObjects.filter { t -> t != target })

    inline fun addCore(i: Int, core: Core) = replaceFO(i) { bso -> BSO(bso, core = bso.core + core) }
    inline fun addCore(bso: BSO, core: Core) = replaceFO(bso) { bso -> BSO(bso, core = bso.core + core) }
    inline fun replaceCore(target: BSO, core: Core) = setFieldObjects(bsObjects.map { t -> if (t == target) BSO(cards = t.cards, core = core, id = t.id) else t })
    inline fun replacePayableFieldObjects(op: (Int, BSO) -> BSO) = replaceFOs { i, t ->
        if (i >= Side_XXX.FOID_CORE_PAYABLE_ORIGIN) op(i - Side_XXX.FOID_CORE_PAYABLE_ORIGIN, t) else t
    }

    inline fun opCore(index: Int, op: (BSO) -> Core) = replaceFO(index) { fo -> BSO(fo, core = op(fo)) }
    inline fun replacePayableCores(cores: List<Core>) = replacePayableFieldObjects { i, fo -> BSO(fo, core = cores[i]) }
    inline fun addTrashCore(c: Core) = opCore(CORETRASH) { bso -> bso.core + c }
    inline fun addReserveCore(c: Core) = opCore(RESERVE) { bso -> bso.core + c }


    inline fun pickDeckTop(n: Int): Sequence<Pair<Cards, Side_XXX>> = deckCards.pickTop(n)
            .map { (pick, rem) -> pick to tr(deck = rem) }

    inline fun putDeckTop(ca: Cards): Side_XXX = tr(deck = deckCards.putTop(ca))
    inline fun putHand(ca: Cards): Side_XXX = tr(deck = handCards.putTop(ca))

    override fun toString() = "{FO:${bsObjects}}"
    inline fun min(a: Int, b: Int) = if (a > b) b else a

    // 以下コア数カード数維持した処理
    inline fun opMoveCore(src: Int, dst: Int): Sequence<Side_XXX> {
        val mvCore = bsObjects[src].core
        return sequenceOf(addCore(src, -mvCore).addCore(dst, mvCore))
    }

    inline fun opMoveCore(src: Int, dst: Int, mvCore: Core): Sequence<Side_XXX> {
        if (mvCore.c > bsObjects[src].core.c || mvCore.s > bsObjects[src].core.s) return sequenceOf()
        return sequenceOf(addCore(src, -mvCore).addCore(dst, mvCore))
    }

    inline fun opMoveCore(src: Int, dst: Int, mvCore: Int): Sequence<Side_XXX> = sequence {
        val srcCore = bsObjects[src].core
        val dstCore = bsObjects[dst].core
        if (mvCore <= srcCore.c) for (s in min(srcCore.s, mvCore) downTo 0) {
            yield(addCore(src, -Core(mvCore, s)).addCore(dst, Core(mvCore, s)))
        }
    }

    inline fun opMoveCore(src: BSO, dst: BSO, mvCore: Int): Sequence<Side_XXX> = sequence {
        if (mvCore <= src.core.c) for (s in min(src.core.s, mvCore) downTo 0) {
            yield(addCore(src, -Core(mvCore, s)).addCore(dst, Core(mvCore, s)))
        }
    }

    inline fun opMoveCore(src: BSO, dst: BSO): Sequence<Side_XXX> = sequence {
        yield(replaceCore(src, Core(0)).replaceCore(dst, dst.core + src.core))
    }


    fun opMoveCardsBy(src: Int, dst: Int, cards: Cards, op: (Cards) -> Cards): Sequence<Side_XXX> = sequence {
        val postSrc = bsObjects[src].cards.drops(cards)
        if (bsObjects[src].cards.size - postSrc.size == cards.size) {
            val post = bsObjects.mapIndexed { i, bso ->
                when (i) {
                    src -> BSO(bso, cards = postSrc)
                    dst -> BSO(bso, cards = op(bso.cards))
                    else -> bso
                }
            }
            yield(this@Side.tr(bsos = post))
        }
    }

    fun opMoveCardsBy(src: Int, dst: Int, op: (Cards, Cards) -> Sequence<Pair<Cards, Cards>>): Sequence<Side_XXX> = sequence {
        op(bsObjects[src].cards, bsObjects[dst].cards).forEach { (postSrc, postDst) ->
            val postSide = replaceFOCards(src) { postSrc }.replaceFOCards(dst) { postDst }
            yield(postSide)
        }
    }

    fun opMoveCard(card: Card, dst: Int): Sequence<Side_XXX> = sequence {
        for (i in 0..bsObjects.size - 1) {
            if (i !== dst) {
                for (c in bsObjects[i].cards) {
                    if (c === card) {
                        yield(replaceFO(i) { BSO(it, cards = it.cards.filter { it !== card }) }.replaceFO(dst) { BSO(it, cards = it.cards + card) })
                    }
                }
            }
        }
    }

    fun opMoveCards(cards: Cards, dst: Int): Sequence<Side_XXX> = sequence {
        if (cards.size == 0) yield(this@Side)
        else opMoveCard(cards[0], dst).forEach { it.opMoveCards(cards.drop(1), dst).forEach { yield(it) } }
    }


    inline fun opMoveCardToTop(src: Int, dst: Int, cards: Cards): Sequence<Side_XXX> = opMoveCardsBy(src, dst, cards) { cs -> cards.reversed() + cs }
    inline fun opMoveCardToBtm(src: Int, dst: Int, cards: Cards): Sequence<Side_XXX> = opMoveCardsBy(src, dst, cards) { cs -> cs + cards }
    inline fun opPickCardsTop(src: Int, n: Int): Sequence<Side_XXX> = sequence<Side_XXX> {
        bsObjects[src].cards.pickTop(n).forEach { (p, r) ->
            yield(replaceFO(src) { BSO(it, cards = r) }.replaceFO(PICKEDCARD) { BSO(it, cards = it.cards + p) })
        }
    }

    inline fun opPickDeck(n: Int) = opPickCardsTop(DECK, n)


    inline fun opMoveCards(bso: Int, n: Int) = opPickCardsTop(DECK, n)
}


//片方の盤面
class Side_XXX(
        val bsObjects: BSOs
) : Comparable<Side_XXX> {
    constructor(
            deck: Cards = listOf()
            , hand: Cards = listOf()
            , burst: Cards = listOf()
            , life: Core = Core(5, 0)
            , trashCore: Core = Core(0)
            , trashCards: Cards = listOf()
            , reserve: Core = Core(4, 0)
            , field: BSOs = listOf()

            , pickedCards: Cards = listOf()
            , pickedCore: Core = Core(0)
    ) : this( // 初期化
            bsObjects = listOf(
                    BSO("Pk", cards = pickedCards) //一時的にピックアップしたカード(オープン等)
                    , BSO("Cp", core = pickedCore) //一時的にピックアップしたコア(コスト支払いなど)
                    , BSO("Dk", cards = deck)
                    , BSO("Hd", cards = hand, ordering = false)
                    , BSO("Bu", cards = burst)
                    , BSO("Tr", cards = trashCards, ordering = false)
                    , BSO("CT", core = trashCore)
                    , BSO("CL", core = life)
                    , BSO("CR", core = reserve)
                    , *field.toTypedArray()
            )
    )

    constructor(
            org: Side_XXX
            , deck: Cards = org.deckCards
            , hand: Cards = org.handCards
            , burst: Cards = org.burst
            , trashCards: Cards = org.trashCards_XXX
            , trashCore: Core = org.trashCore_XXX
            , life: Core = org.life
            , reserve: Core = org.reserve
            , field: BSOs = org.field

            , pickedCards: Cards = org.picked_Cards
            , pickedCore: Core = org.pickedCore
    ) : this(
            deck = deck
            , hand = hand
            , burst = burst
            , life = life
            , trashCards = trashCards
            , trashCore = trashCore
            , reserve = reserve
            , field = field

            , pickedCards = pickedCards
            , pickedCore = pickedCore
    )

    companion object {
        val PICKEDCARD = 0
        val PICKEDCORE = 1
        val DECK = 2
        val HAND = 3
        val BURST = 4
        val CARDTRASH = 5
        val CORETRASH = 6
        val LIFE = 7
        val RESERVE = 8

        val FOID_CORE_PAYABLE_ORIGIN = 8
        val FOID_FIELD_ORIGIN = 9


    }

    override fun hashCode(): Int {
        return bsObjects.subList(0, FOID_FIELD_ORIGIN - 1).fold(0) { s, t ->
            s * 31 + t.hashCode()
        } + field.fold(0) { s, t ->
            t.hashCode() + s
        }
    }

    override fun equals(o: Any?) = compareTo(o as Side_XXX) == 0

    fun BSOs.compareTo(o: BSOs): Int {
        zip(o).asSequence().forEach { it.first.compareTo(it.second).let { if (it != 0) return it } }
        return 0
    }

    override fun compareTo(o: Side_XXX): Int {
        (bsObjects.size - o.bsObjects.size).let { if (it != 0) return it }
        bsObjects.zip(o.bsObjects).asSequence().forEach { it.first.compareTo(it.second).let { if (it != 0) return it } }
        return 0
    }

    inline fun tr(bsos: BSOs) = Side_XXX(bsObjects = bsObjects)
    inline fun trBy(op: (BSO) -> BSO): Side_XXX = Side_XXX(bsObjects.map { op(it) })

    inline fun tr(
            deck: Cards = this.deckCards
            , hand: Cards = this.handCards
            , burst: Cards = this.burst
            , trashCards: Cards = this.trashCards_XXX
            , trashCore: Core = this.trashCore_XXX
            , life: Core = this.life
            , reserve: Core = this.reserve
            , field: BSOs = this.field

            , pickedCards: Cards = this.picked_Cards
            , pickedCore: Core = this.pickedCore
    ) = Side_XXX(
            deck = deck
            , hand = hand
            , burst = burst
            , life = life
            , trashCards = trashCards
            , trashCore = trashCore
            , reserve = reserve
            , field = field

            , pickedCards = pickedCards
            , pickedCore = pickedCore
    )

    inline fun checkCoreCard(): Pair<Core, Int> = bsObjects.fold(Core(0) to 0) { (cores, cards), t -> (cores + t.core) to (cards + t.cards.size) }


    inline val picked_Cards get() = bsObjects[PICKEDCARD].cards
    inline val pickedCards get() = bsObjects[PICKEDCARD]
    inline val pickedCore: Core get() = bsObjects[PICKEDCORE].core
    inline val deck: BSO get() = bsObjects[DECK]
    inline val deckCards: Cards get() = deck.cards
    inline val hand: BSO get() = bsObjects[HAND]
    inline val handCards: Cards get() = hand.cards
    inline val burst: Cards get() = bsObjects[BURST].cards
    inline val trashCards_XXX: Cards get() = bsObjects[CARDTRASH].cards
    inline val trashCards: BSO get() = bsObjects[CARDTRASH]
    inline val trashCore_XXX: Core get() = bsObjects[CORETRASH].core
    inline val trashCore: BSO get() = bsObjects[CORETRASH]
    inline val life: Core get() = bsObjects[LIFE].core
    inline val reserve: Core get() = bsObjects[RESERVE].core

    inline val field: BSOs get() = bsObjects.drop(FOID_FIELD_ORIGIN)
    inline val payableCoreHolder get() = bsObjects.subList(FOID_CORE_PAYABLE_ORIGIN, bsObjects.size).map { it.core }
    inline val fieldSimbols get() = field.fold(Sbl()) { s, fo -> s + if (fo.cards.size != 0) fo.cards[0].simbols else Sbl.Zero }

    inline fun replaceBSOsBy(op: Side_XXX.(BSO) -> BSO) = Side_XXX(bsObjects.map { t -> this.op(t) })
    inline fun replaceBSOsBy(dst: BSO, op: (BSO) -> BSO) = tr(bsos = bsObjects.map { t -> if (t === dst) op(t) else t })
    inline fun replaceBSOsBy(bsosDst: BSOs, op: (BSO) -> BSO) = tr(bsos = bsObjects.map { t -> if (bsosDst.contains(t)) op(t) else t })
    inline fun replaceBSOs(m: Map<BSO, BSO>): Side_XXX = tr(bsObjects.map { m[it] ?: it })

    inline fun replaceCoreBy(dst: BSO, op: (Core) -> Core) = replaceBSOsBy(dst) { bso -> bso.tr(core = op(bso.core)) }

    inline fun moveCardsBy(dst: BSO, cs: Cards, op: (Cards) -> Cards): Side_XXX =
            replaceBSOsBy { o ->
                if (o === dst) {
                    o.tr(cards = op(o.cards))
                } else if (o.cards.intersect(cs).size == 0) {
                    o
                } else {
                    o.tr(cards = o.cards.drops(cs))
                }
            }

    // コア取り出し可能なBSOからコアの移動
    inline fun opMoveCore(bso: BSO, cost: Int): Sequence<Side_XXX> = sequence {
        payableCoreHolder.pickCore(cost).forEach { (picked, rem) -> // コスト支払い可能なBSOからコアを抽出
            yield(replacePayableCores(rem).replaceCoreBy(bso) { it + picked }) // 抽出したコアを指定BSOに置く
        }
    }


    // コストを支払う
    inline fun opPayCost(cost: Int): Sequence<Side_XXX> = opMoveCore(trashCore, cost) // コア取り出し可能なBSOからコアを集めてトラッシュに移動

    // ------------- Old API

    inline fun replaceFoBy(op: (BSOs) -> BSOs) = this.tr(bsos = op(bsObjects))
    inline fun setFieldObjects(fos: List<BSO>) = this.tr(bsos = fos)
    inline fun replaceFOs(op: (Int, BSO) -> BSO) = this.tr(bsos = bsObjects.mapIndexed { i, t -> op(i, t) })
    inline fun replaceFOIndexed(index: Int, op: (Int, BSO) -> BSO) = replaceFOs { i, t -> if (i == index) op(i, t) else t }
    inline fun replaceFO(index: Int, op: (BSO) -> BSO) = replaceFOs { i, t -> if (i == index) op(t) else t }
    inline fun replaceFO(bso: BSO, op: (BSO) -> BSO) = replaceFOs { i, t -> if (t === bso) op(t) else t }
    inline fun replaceFOCards(i: Int, op: (Cards) -> Cards) = replaceFO(i) { BSO(it, cards = op(it.cards)) }
    inline fun replaceFOCore(i: Int, op: (Core) -> Core) = replaceFO(i) { BSO(it, core = op(it.core)) }

    inline fun createFieldObject(newObject: BSO) = setFieldObjects(bsObjects + listOf(newObject))
    inline fun createFieldObject(cards: List<Card>, core: Core, id: String) = setFieldObjects(bsObjects + listOf(BSO(id = id, cards = cards, core = core)))
    inline fun delFieldObject(index: Int) = setFieldObjects(bsObjects.filterIndexed { i, _ -> i != index })
    inline fun delFieldObject(target: BSO) = setFieldObjects(bsObjects.filter { t -> t != target })

    inline fun addCore(i: Int, core: Core) = replaceFO(i) { bso -> BSO(bso, core = bso.core + core) }
    inline fun addCore(bso: BSO, core: Core) = replaceFO(bso) { bso -> BSO(bso, core = bso.core + core) }
    inline fun replaceCore(target: BSO, core: Core) = setFieldObjects(bsObjects.map { t -> if (t == target) BSO(cards = t.cards, core = core, id = t.id) else t })
    inline fun replacePayableFieldObjects(op: (Int, BSO) -> BSO) = replaceFOs { i, t ->
        if (i >= Side_XXX.FOID_CORE_PAYABLE_ORIGIN) op(i - Side_XXX.FOID_CORE_PAYABLE_ORIGIN, t) else t
    }

    inline fun opCore(index: Int, op: (BSO) -> Core) = replaceFO(index) { fo -> BSO(fo, core = op(fo)) }
    inline fun replacePayableCores(cores: List<Core>) = replacePayableFieldObjects { i, fo -> BSO(fo, core = cores[i]) }
    inline fun addTrashCore(c: Core) = opCore(CORETRASH) { bso -> bso.core + c }
    inline fun addReserveCore(c: Core) = opCore(RESERVE) { bso -> bso.core + c }


    inline fun pickDeckTop(n: Int): Sequence<Pair<Cards, Side_XXX>> = deckCards.pickTop(n)
            .map { (pick, rem) -> pick to tr(deck = rem) }

    inline fun putDeckTop(ca: Cards): Side_XXX = tr(deck = deckCards.putTop(ca))
    inline fun putHand(ca: Cards): Side_XXX = tr(deck = handCards.putTop(ca))

    override fun toString() = "{FO:${bsObjects}}"
    inline fun min(a: Int, b: Int) = if (a > b) b else a

    // 以下コア数カード数維持した処理
    inline fun opMoveCore(src: Int, dst: Int): Sequence<Side_XXX> {
        val mvCore = bsObjects[src].core
        return sequenceOf(addCore(src, -mvCore).addCore(dst, mvCore))
    }

    inline fun opMoveCore(src: Int, dst: Int, mvCore: Core): Sequence<Side_XXX> {
        if (mvCore.c > bsObjects[src].core.c || mvCore.s > bsObjects[src].core.s) return sequenceOf()
        return sequenceOf(addCore(src, -mvCore).addCore(dst, mvCore))
    }

    inline fun opMoveCore(src: Int, dst: Int, mvCore: Int): Sequence<Side_XXX> = sequence {
        val srcCore = bsObjects[src].core
        val dstCore = bsObjects[dst].core
        if (mvCore <= srcCore.c) for (s in min(srcCore.s, mvCore) downTo 0) {
            yield(addCore(src, -Core(mvCore, s)).addCore(dst, Core(mvCore, s)))
        }
    }

    inline fun opMoveCore(src: BSO, dst: BSO, mvCore: Int): Sequence<Side_XXX> = sequence {
        if (mvCore <= src.core.c) for (s in min(src.core.s, mvCore) downTo 0) {
            yield(addCore(src, -Core(mvCore, s)).addCore(dst, Core(mvCore, s)))
        }
    }

    inline fun opMoveCore(src: BSO, dst: BSO): Sequence<Side_XXX> = sequence {
        yield(replaceCore(src, Core(0)).replaceCore(dst, dst.core + src.core))
    }


    fun opMoveCardsBy(src: Int, dst: Int, cards: Cards, op: (Cards) -> Cards): Sequence<Side_XXX> = sequence {
        val postSrc = bsObjects[src].cards.drops(cards)
        if (bsObjects[src].cards.size - postSrc.size == cards.size) {
            val post = bsObjects.mapIndexed { i, bso ->
                when (i) {
                    src -> BSO(bso, cards = postSrc)
                    dst -> BSO(bso, cards = op(bso.cards))
                    else -> bso
                }
            }
            yield(this@Side_XXX.tr(bsos = post))
        }
    }

    fun opMoveCardsBy(src: Int, dst: Int, op: (Cards, Cards) -> Sequence<Pair<Cards, Cards>>): Sequence<Side_XXX> = sequence {
        op(bsObjects[src].cards, bsObjects[dst].cards).forEach { (postSrc, postDst) ->
            val postSide = replaceFOCards(src) { postSrc }.replaceFOCards(dst) { postDst }
            yield(postSide)
        }
    }

    fun opMoveCard(card: Card, dst: Int): Sequence<Side_XXX> = sequence {
        for (i in 0..bsObjects.size - 1) {
            if (i !== dst) {
                for (c in bsObjects[i].cards) {
                    if (c === card) {
                        yield(replaceFO(i) { BSO(it, cards = it.cards.filter { it !== card }) }.replaceFO(dst) { BSO(it, cards = it.cards + card) })
                    }
                }
            }
        }
    }

    fun opMoveCards(cards: Cards, dst: Int): Sequence<Side_XXX> = sequence {
        if (cards.size == 0) yield(this@Side_XXX)
        else opMoveCard(cards[0], dst).forEach { it.opMoveCards(cards.drop(1), dst).forEach { yield(it) } }
    }


    inline fun opMoveCardToTop(src: Int, dst: Int, cards: Cards): Sequence<Side_XXX> = opMoveCardsBy(src, dst, cards) { cs -> cards.reversed() + cs }
    inline fun opMoveCardToBtm(src: Int, dst: Int, cards: Cards): Sequence<Side_XXX> = opMoveCardsBy(src, dst, cards) { cs -> cs + cards }
    inline fun opPickCardsTop(src: Int, n: Int): Sequence<Side_XXX> = sequence<Side_XXX> {
        bsObjects[src].cards.pickTop(n).forEach { (p, r) ->
            yield(replaceFO(src) { BSO(it, cards = r) }.replaceFO(PICKEDCARD) { BSO(it, cards = it.cards + p) })
        }
    }

    inline fun opPickDeck(n: Int) = opPickCardsTop(DECK, n)


    inline fun opMoveCards(bso: Int, n: Int) = opPickCardsTop(DECK, n)
}


fun Sequence<Side_XXX>.opMoveCardsBy(src: Int, dst: Int, op: (Cards, Cards) -> Sequence<Pair<Cards, Cards>>): Sequence<Side_XXX> = flatMap {
    it.opMoveCardsBy(src, dst, op)
}



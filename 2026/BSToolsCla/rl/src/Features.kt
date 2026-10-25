package bstools.valuerl

import bstools.model.*

/**
 * 局面 → ネットワーク入力 (可変長カードトークン列 + 大域特徴) への変換。
 *
 * すべて「評価する側のプレイヤー(me)」視点。相手手札の中身・両者デッキの中身は非公開情報なので
 * 使わない (総合ルール 4-2-2 / 4-3-2)。トラッシュは公開情報 (4-4-2) なので両者とも使う。
 *
 * トークンの並びは「変化しにくい順」(相手トラッシュ → 自分トラッシュ → 相手の場 → 自分の場 → 手札)。
 * 1手先読みでは候補ごとに末尾側(自分の場・手札)だけが変わることが多いため、
 * [GruValueNet.evaluateBatch] の接頭辞キャッシュがよく効く。
 */

/** 所在ゾーン (one-hot) */
private const val Z_HAND = 0
private const val Z_MY_FIELD = 1
private const val Z_OPP_FIELD = 2
private const val Z_MY_TRASH = 3
private const val Z_OPP_TRASH = 4
private const val ZONES = 5

/** トークン次元: ゾーン one-hot(5) + 数値特徴(8) + 種別 one-hot(3) */
const val TOKEN_DIM = ZONES + 8 + 3

/** 大域特徴の次元数 */
const val GLOBAL_DIM = 20

/**
 * @param tokens トークンごとの能力値 (TOKEN_DIM 次元)
 * @param globals 大域特徴 (GLOBAL_DIM 次元)
 * @param cardIds トークンごとのカード番号のハッシュ値 (ネットワーク側で埋め込みのバケットに割り当てる)
 */
class Features(val tokens: List<DoubleArray>, val globals: DoubleArray, val cardIds: IntArray)

/** カード番号 (例: "26RSD03-007") の安定したハッシュ値 (FNV-1a 32bit)。実行ごとに変わらない */
fun cardHash(cardNo: String): Int {
    var h = -0x7ee3623b // 2166136261
    for (ch in cardNo) {
        h = h xor ch.code
        h *= 16777619
    }
    return h and 0x7fffffff
}

private fun categoryOneHot(t: DoubleArray, base: Int, c: CardCategory) {
    when (c) {
        CardCategory.SPIRIT -> t[base] = 1.0
        CardCategory.NEXUS -> t[base + 1] = 1.0
        else -> t[base + 2] = 1.0
    }
}

private fun staticToken(card: CardData, zone: Int): DoubleArray {
    val t = DoubleArray(TOKEN_DIM)
    t[zone] = 1.0
    t[ZONES + 0] = card.cost / 10.0
    t[ZONES + 6] = card.symbols.size / 3.0
    t[ZONES + 7] = card.reductionSymbols.size / 3.0
    categoryOneHot(t, ZONES + 8, card.category)
    return t
}

private fun fieldToken(fc: FieldCard, zone: Int): DoubleArray {
    val t = DoubleArray(TOKEN_DIM)
    t[zone] = 1.0
    t[ZONES + 0] = fc.cost / 10.0
    t[ZONES + 1] = fc.currentBp / 10000.0
    t[ZONES + 2] = fc.level / 3.0
    t[ZONES + 3] = fc.cores.total / 10.0
    t[ZONES + 4] = if (fc.cores.soul > 0) 1.0 else 0.0
    t[ZONES + 5] = if (fc.isExhausted) 1.0 else 0.0
    t[ZONES + 6] = fc.symbols.size / 3.0
    categoryOneHot(t, ZONES + 8, fc.category)
    return t
}

fun extractFeatures(state: GameState, meId: Int): Features {
    val me = if (meId == 1) state.player1 else state.player2
    val opp = if (meId == 1) state.player2 else state.player1

    val tokens = ArrayList<DoubleArray>(me.hand.size + me.field.size + opp.field.size + me.trashCards.size + opp.trashCards.size)
    val ids = ArrayList<Int>(tokens.size)
    for (c in opp.trashCards) { tokens.add(staticToken(c, Z_OPP_TRASH)); ids.add(cardHash(c.cardNo)) }
    for (c in me.trashCards) { tokens.add(staticToken(c, Z_MY_TRASH)); ids.add(cardHash(c.cardNo)) }
    for (fc in opp.field) { tokens.add(fieldToken(fc, Z_OPP_FIELD)); ids.add(cardHash(fc.cardNo)) }
    for (fc in me.field) { tokens.add(fieldToken(fc, Z_MY_FIELD)); ids.add(cardHash(fc.cardNo)) }
    for (c in me.hand) { tokens.add(staticToken(c, Z_HAND)); ids.add(cardHash(c.cardNo)) }

    val g = DoubleArray(GLOBAL_DIM)
    g[0] = me.life / 5.0
    g[1] = opp.life / 5.0
    g[2] = me.reserve.total / 10.0
    g[3] = opp.reserve.total / 10.0
    g[4] = if (me.reserve.soul > 0) 1.0 else 0.0
    g[5] = if (opp.reserve.soul > 0) 1.0 else 0.0
    g[6] = me.trash.total / 10.0
    g[7] = opp.trash.total / 10.0
    g[8] = me.hand.size / 10.0
    g[9] = opp.hand.size / 10.0 // 枚数のみ (公開情報)
    g[10] = me.deckCount / 40.0
    g[11] = opp.deckCount / 40.0
    g[12] = me.field.sumOf { it.symbols.size } / 5.0
    g[13] = opp.field.sumOf { it.symbols.size } / 5.0
    g[14] = state.turn / 20.0
    g[15] = if (state.activePlayerId == meId) 1.0 else 0.0
    when (state.step) {
        Step.MAIN, Step.MAIN_2 -> g[16] = 1.0
        Step.ATTACK_DECLARATION, Step.ATTACK_START, Step.FLASH_TIMING -> g[17] = 1.0
        Step.BLOCK_DECLARATION, Step.BATTLE_RESOLUTION -> g[18] = 1.0
        else -> {}
    }
    g[19] = 1.0 // バイアス
    return Features(tokens, g, ids.toIntArray())
}

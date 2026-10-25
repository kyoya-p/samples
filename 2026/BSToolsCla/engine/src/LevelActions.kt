package bstools.model

/**
 * Lv単位の正規形列挙 (`--actions level`)。
 *
 * 一括再配置 ([enumerateRearrangements]) は配置の全組み合わせ N×C(X+N−2, N−1) を1手ずつ列挙するため、
 * 終盤 (場所7・コア12で86,632通り、場所8・コア15で930,240通り) に件数と計算量が爆発する。
 * ここでは同じ結果に到達できる手を、次の小さな手の組み合わせとして列挙する:
 *
 *  - **プレイ**: 支払いはリザーブから (ソウルコアで払う / 払わない)。配置するコアは Lv の境目の数
 *    (最低Lv以上の各LvのLvコスト) で、ソウルコアを含める / 含めない
 *  - **Lv変更**: 自分のフィールドの各カードについて「コアをそのLvの境目の数にする」(0 = スピリットは消滅)。
 *    差分の通常コアをリザーブとの間で動かす。ソウルコアは動かさない
 *  - **ソウルコアの移動**: ソウルコアを別の場所 (リザーブ・各カード) へ1個動かす
 *
 * 同じカードのLv変更・ソウルコアの移動は、プレイ等の間に1回ずつだけ ([GameState.coreWindow])。
 * 2回続けても1回で同じ結果にできるので、選べる結果は減らず、コアを動かし続ける循環は構造上起きない。
 *
 * 到達できる結果: 各カードのLv (またはスピリットの消滅) とソウルコアの場所のすべての組み合わせ。
 * Lvの境目を超える余りのコアは常にリザーブに置く形にそろえる。今のルールエンジン
 * (カード効果は未実装) では、カードの上の余りのコアとリザーブのコアは支払い・Lvアップ・破壊時の行き先の
 * どれでも同じ扱いなので、結果は同じになる (フィールドのコアを使う支払いも「Lv変更で下げてからリザーブで払う」に同じ)。
 */
fun enumerateLevelActions(state: GameState): List<GameAction> {
    if (state.winner != null) return emptyList()
    if (state.step != Step.MAIN && state.step != Step.MAIN_2) return markRepetitions(state, enumerateRawActions(state))
    val p = state.choosingPlayer
    val prefix = if (state.step == Step.MAIN_2) "[第2メイン] " else ""
    val acts = ArrayList<GameAction>()
    for (handIdx in p.hand.indices.distinctBy { p.hand[it].cardNo }) {
        acts.addAll(levelPlays(p, p.hand[handIdx], handIdx, prefix))
    }
    acts.addAll(levelChanges(state, p))
    acts.addAll(soulMoves(state, p))
    return markRepetitions(state, acts).mapIndexed { i, a -> a.copy(index = i + 1) }
}

private fun keepOf(fc: FieldCard): Int = if (fc.category == CardCategory.NEXUS) 0 else maxOf(1, fc.lvCosts.firstOrNull() ?: 1)

private fun lvText(fc: FieldCard, total: Int, soul: Int): String =
    if (total < keepOf(fc)) "消滅" else "Lv${levelFor(fc.lvCosts, total, soul, fc.shinkai)}"

/** プレイ: リザーブから支払い、Lvの境目の数のコアを置く */
private fun levelPlays(p: PlayerState, card: CardData, handIdx: Int, prefix: String): List<GameAction> {
    val cost = summonCost(card, p)
    val category = when (card.category) {
        CardCategory.MAGIC -> "✨【${prefix}マジック使用】 ${card.name}"
        CardCategory.NEXUS -> "🌐【${prefix}配置】 ${card.name}"
        else -> "🎴【${prefix}召喚】 ${card.name}"
    }
    val actionType = if (card.category == CardCategory.MAGIC) "USE_MAGIC" else "SUMMON"
    val r = p.reserve
    val out = ArrayList<GameAction>()
    for (paySoul in 0..(if (cost > 0) minOf(1, r.soul) else 0)) {
        val payNormal = cost - paySoul
        if (payNormal < 0 || payNormal > r.normal) continue
        val restNormal = r.normal - payNormal
        val restSoul = r.soul - paySoul
        val payFrom = if (cost > 0) listOf(CoreAlloc("Reserve", "R", payNormal, paySoul)) else emptyList()
        val payLabel = if (cost == 0) "コスト不要" else "コスト${coreIcons(payNormal, paySoul)}"
        // 末尾の括弧は WebUI がクリックで選んだコアと突き合わせる (全列挙と同じ「場所:個数アイコン」の形式)
        val payStr = if (cost > 0) "R:${coreIcons(payNormal, paySoul)}" else "なし"
        if (card.category == CardCategory.MAGIC) {
            out.add(GameAction(0, actionType, category, "$payLabel, コアは置かない, (支払:$payStr, 配置:なし)",
                handIndex = handIdx, costToPay = cost, payFrom = payFrom))
            continue
        }
        val minimum = requiredCoresOnSummon(card)
        val thresholds = (listOf(minimum) + card.lvCosts.filter { it > minimum }).distinct().sorted()
        for (t in thresholds) {
            for (placeSoul in 0..minOf(1, restSoul)) {
                if (t == 0 && placeSoul == 1) continue
                val normal = t - placeSoul
                if (normal < 0 || normal > restNormal) continue
                val lv = levelFor(card.lvCosts, t, placeSoul, card.shinkai)
                val placeFrom = if (t > 0) listOf(CoreAlloc("Reserve", "R", normal, placeSoul)) else emptyList()
                val placeLabel = if (t == 0) "配置コアなし" else "配置コア${coreIcons(normal, placeSoul)}"
                val placeStr = if (t == 0) "なし" else "R:${coreIcons(normal, placeSoul)}"
                out.add(GameAction(0, actionType, category, "$payLabel, $placeLabel → Lv$lv, (支払:$payStr, 配置:$placeStr)",
                    handIndex = handIdx, costToPay = cost, payFrom = payFrom, placeFrom = placeFrom))
            }
        }
    }
    return out
}

/** Lv変更: 各カードのコアを、そのLvの境目の数 (0 = スピリットは消滅) にする。ソウルコアは動かさない */
private fun levelChanges(state: GameState, p: PlayerState): List<GameAction> {
    val out = ArrayList<GameAction>()
    for (fc in p.field) {
        if (fc.instanceId in state.coreWindow) continue
        val soulOn = fc.cores.soul
        val targets = (listOf(0) + fc.lvCosts.filter { it > 0 }).distinct().sorted()
        for (t in targets) {
            if (t == fc.cores.total || t < soulOn) continue
            val newNormal = t - soulOn
            val delta = newNormal - fc.cores.normal
            if (delta > 0 && p.reserve.normal < delta) continue
            val move = if (delta < 0) CoreMove(fc.instanceId, "Reserve", fc.displayName, "R", -delta, 0)
                else CoreMove("Reserve", fc.instanceId, "R", fc.displayName, delta, 0)
            val vanish = t < keepOf(fc)
            out.add(GameAction(0, "MOVE_CORE", "🔷【Lv変更】 ${fc.displayName}",
                "${coreIcons(newNormal, soulOn).ifEmpty { "0個" }} → ${lvText(fc, t, soulOn)}" +
                    (if (delta < 0) " (R へ ${coreIcons(-delta, 0)})" else " (R から ${coreIcons(delta, 0)})") +
                    (if (vanish) " ⚠️${fc.displayName}消滅" else ""),
                targetInstanceId = fc.instanceId, moves = listOf(move), rearrangeKey = fc.instanceId))
        }
    }
    return out
}

/** ソウルコアの移動: 今ある場所から別の場所 (リザーブ・各カード) へ */
private fun soulMoves(state: GameState, p: PlayerState): List<GameAction> {
    if ("SOUL" in state.coreWindow) return emptyList()
    val fromCard = p.field.firstOrNull { it.cores.soul > 0 }
    val fromId = if (p.reserve.soul > 0) "Reserve" else fromCard?.instanceId ?: return emptyList()
    val fromLabel = if (fromId == "Reserve") "R" else fromCard!!.displayName
    val out = ArrayList<GameAction>()
    val dests = listOf<FieldCard?>(null) + p.field
    for (dst in dests) {
        val toId = dst?.instanceId ?: "Reserve"
        if (toId == fromId) continue
        val toLabel = dst?.displayName ?: "R"
        val notes = ArrayList<String>()
        if (fromId != "Reserve") notes.add("$fromLabel ${lvText(fromCard!!, fromCard.cores.total - 1, 0)}")
        if (dst != null) notes.add("$toLabel ${lvText(dst, dst.cores.total + 1, 1)}")
        val vanish = fromId != "Reserve" && fromCard!!.cores.total - 1 < keepOf(fromCard)
        out.add(GameAction(0, "MOVE_CORE", "🔶【ソウルコア移動】",
            "$fromLabel ➔ $toLabel 🔶" + (if (notes.isEmpty()) "" else " (${notes.joinToString(", ")})") +
                (if (vanish) " ⚠️${fromLabel}消滅" else ""),
            targetInstanceId = dst?.instanceId, moves = listOf(CoreMove(fromId, toId, fromLabel, toLabel, normal = 0, soul = 1)),
            rearrangeKey = "SOUL"))
    }
    return out
}

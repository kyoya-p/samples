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
    acts.addAll(soulSwaps(state, p))
    val distinctActs = acts.distinctBy { actionKey(it) }
    return markRepetitions(state, distinctActs).mapIndexed { i, a -> a.copy(index = i + 1) }
}

private fun actionKey(a: GameAction): String = when (a.type) {
    "MOVE_CORE" -> "MOVE:${a.category}:${a.targetInstanceId}:${a.moves.joinToString { "${it.fromId}->${it.toId}:${it.normal},${it.soul}" }}"
    "SUMMON", "USE_MAGIC" -> "PLAY:${a.category}:${a.handIndex}:pay[${a.payFrom.joinToString { "${it.sourceId}:${it.normal},${it.soul}" }}]:place[${a.placeFrom.joinToString { "${it.sourceId}:${it.normal},${it.soul}" }}]"
    else -> "${a.type}:${a.category}:${a.targetInstanceId}:${a.detail}"
}

private fun keepOf(fc: FieldCard): Int = fc.lvCosts.firstOrNull() ?: 0

private fun lvText(fc: FieldCard, total: Int, soul: Int): String =
    if (total < keepOf(fc)) "消滅" else "Lv${levelFor(fc.lvCosts, total, soul, fc.shinkai)}"

/** プレイ: リザーブおよびフィールド各カードから支払い、Lvの境目の数のコアを置く */
private fun levelPlays(p: PlayerState, card: CardData, handIdx: Int, prefix: String): List<GameAction> {
    val cost = summonCost(card, p)
    val category = when (card.category) {
        CardCategory.MAGIC -> "✨【${prefix}マジック使用】 ${card.name}"
        CardCategory.NEXUS -> "🌐【${prefix}配置】 ${card.name}"
        else -> "🎴【${prefix}召喚】 ${card.name}"
    }
    val actionType = if (card.category == CardCategory.MAGIC) "USE_MAGIC" else "SUMMON"
    val sources = coreSources(p)
    val totalAvailable = sources.sumOf { it.availNormal + it.availSoul }
    val maxPlace = totalAvailable - cost
    if (maxPlace < 0) return emptyList()

    val out = ArrayList<GameAction>()
    if (card.category == CardCategory.MAGIC) {
        return playOptions(card, handIdx, category, actionType, cost, 0, sources, limit = 50)
    }

    val minimum = requiredCoresOnSummon(card)
    val maxCost = card.lvCosts.maxOrNull() ?: 1
    val thresholds = (minimum..minOf(maxCost, maxPlace)).toList()

    for (t in thresholds) {
        out.addAll(playOptions(card, handIdx, category, actionType, cost, t, sources, limit = 50))
    }
    return out
}

/** コア移動・Lv変更: リザーブおよびフィールド各カード間で、移動可能なコア数（通常コア・ソウルコア混合含む）をすべて列挙する */
private fun levelChanges(state: GameState, p: PlayerState): List<GameAction> {
    val out = ArrayList<GameAction>()

    // 1. 各フィールドカードからリザーブへコアを戻す
    for (fc in p.field) {
        val maxNormal = fc.cores.normal
        val hasSoul = fc.cores.soul > 0

        // 通常コアのみ戻す
        for (n in 1..maxNormal) {
            val afterTotal = fc.cores.total - n
            val afterSoul = fc.cores.soul
            val afterNormal = fc.cores.normal - n
            val vanish = afterTotal < keepOf(fc)
            val move = CoreMove(fc.instanceId, "Reserve", fc.displayName, "R", n, 0)
            out.add(GameAction(0, "MOVE_CORE", "🔷【コア移動】 ${fc.displayName} ➔ R",
                "${coreIcons(afterNormal, afterSoul).ifEmpty { "0個" }} → ${lvText(fc, afterTotal, afterSoul)} (R へ ${coreIcons(n, 0)})" +
                    (if (vanish) " ⚠️${fc.displayName}消滅" else ""),
                targetInstanceId = fc.instanceId, moves = listOf(move), rearrangeKey = "${fc.instanceId}->Reserve"))
        }

        // ソウルコアを含む戻し
        if (hasSoul) {
            for (n in 0..maxNormal) {
                val afterTotal = fc.cores.total - (n + 1)
                val afterSoul = 0
                val afterNormal = fc.cores.normal - n
                val vanish = afterTotal < keepOf(fc)
                val move = CoreMove(fc.instanceId, "Reserve", fc.displayName, "R", n, 1)
                val movedDesc = if (n > 0) "${coreIcons(n, 1)}" else "🔶"
                out.add(GameAction(0, "MOVE_CORE", "🔷【コア移動】 ${fc.displayName} ➔ R",
                    "${coreIcons(afterNormal, afterSoul).ifEmpty { "0個" }} → ${lvText(fc, afterTotal, afterSoul)} (R へ $movedDesc)" +
                        (if (vanish) " ⚠️${fc.displayName}消滅" else ""),
                    targetInstanceId = fc.instanceId, moves = listOf(move), rearrangeKey = "${fc.instanceId}->Reserve"))
            }
        }
    }

    // 2. リザーブから各フィールドカードへコアを移動する
    for (fc in p.field) {
        val maxNormal = p.reserve.normal
        val hasSoul = p.reserve.soul > 0

        // 通常コアのみ移動
        for (n in 1..maxNormal) {
            val afterNormal = fc.cores.normal + n
            val afterSoul = fc.cores.soul
            val afterTotal = fc.cores.total + n
            val move = CoreMove("Reserve", fc.instanceId, "R", fc.displayName, n, 0)
            out.add(GameAction(0, "MOVE_CORE", "🔷【コア移動】 R ➔ ${fc.displayName}",
                "${coreIcons(afterNormal, afterSoul)} → ${lvText(fc, afterTotal, afterSoul)} (R から ${coreIcons(n, 0)})",
                targetInstanceId = fc.instanceId, moves = listOf(move), rearrangeKey = "Reserve->${fc.instanceId}"))
        }

        // ソウルコアを含む移動
        if (hasSoul) {
            for (n in 0..maxNormal) {
                val afterNormal = fc.cores.normal + n
                val afterSoul = fc.cores.soul + 1
                val afterTotal = fc.cores.total + n + 1
                val move = CoreMove("Reserve", fc.instanceId, "R", fc.displayName, n, 1)
                val movedDesc = if (n > 0) "${coreIcons(n, 1)}" else "🔶"
                out.add(GameAction(0, "MOVE_CORE", "🔷【コア移動】 R ➔ ${fc.displayName}",
                    "${coreIcons(afterNormal, afterSoul)} → ${lvText(fc, afterTotal, afterSoul)} (R から $movedDesc)",
                    targetInstanceId = fc.instanceId, moves = listOf(move), rearrangeKey = "Reserve->${fc.instanceId}"))
            }
        }
    }

    // 3. フィールドカード間でコアを直接移動する (donor ➔ fc)
    for (donor in p.field) {
        val maxNormal = donor.cores.normal
        val hasSoul = donor.cores.soul > 0

        for (fc in p.field) {
            if (donor.instanceId == fc.instanceId) continue

            // 通常コアのみ移動
            for (n in 1..maxNormal) {
                val donorAfterTotal = donor.cores.total - n
                val donorVanish = donorAfterTotal < keepOf(donor)
                val donorLv = lvText(donor, donorAfterTotal, donor.cores.soul)

                val fcAfterNormal = fc.cores.normal + n
                val fcAfterSoul = fc.cores.soul
                val fcAfterTotal = fc.cores.total + n

                val move = CoreMove(donor.instanceId, fc.instanceId, donor.displayName, fc.displayName, n, 0)
                out.add(GameAction(0, "MOVE_CORE", "🔷【コア移動】 ${donor.displayName} ➔ ${fc.displayName}",
                    "${coreIcons(fcAfterNormal, fcAfterSoul)} → ${lvText(fc, fcAfterTotal, fcAfterSoul)} (${donor.displayName}から${coreIcons(n, 0)}: $donorLv)" +
                        (if (donorVanish) " ⚠️${donor.displayName}消滅" else ""),
                    targetInstanceId = fc.instanceId, moves = listOf(move), rearrangeKey = "${donor.instanceId}->${fc.instanceId}"))
            }

            // ソウルコアを含む移動 (通常コア n 個 + ソウルコア 1 個)
            if (hasSoul) {
                for (n in 0..maxNormal) {
                    val donorAfterTotal = donor.cores.total - (n + 1)
                    val donorVanish = donorAfterTotal < keepOf(donor)
                    val donorLv = lvText(donor, donorAfterTotal, 0)

                    val fcAfterNormal = fc.cores.normal + n
                    val fcAfterSoul = fc.cores.soul + 1
                    val fcAfterTotal = fc.cores.total + n + 1

                    val movedDesc = if (n > 0) "${coreIcons(n, 1)}" else "🔶"
                    val move = CoreMove(donor.instanceId, fc.instanceId, donor.displayName, fc.displayName, n, 1)
                    out.add(GameAction(0, "MOVE_CORE", "🔷【コア移動】 ${donor.displayName} ➔ ${fc.displayName}",
                        "${coreIcons(fcAfterNormal, fcAfterSoul)} → ${lvText(fc, fcAfterTotal, fcAfterSoul)} (${donor.displayName}から$movedDesc: $donorLv)" +
                            (if (donorVanish) " ⚠️${donor.displayName}消滅" else ""),
                        targetInstanceId = fc.instanceId, moves = listOf(move), rearrangeKey = "${donor.instanceId}->${fc.instanceId}"))
                }
            }
        }
    }

    return out
}

/** ソウルコアと通常コアの交換 */
private fun soulSwaps(state: GameState, p: PlayerState): List<GameAction> {
    val fromCard = p.field.firstOrNull { it.cores.soul > 0 }
    val fromId = if (p.reserve.soul > 0) "Reserve" else fromCard?.instanceId ?: return emptyList()
    val fromLabel = if (fromId == "Reserve") "R" else fromCard!!.displayName
    val out = ArrayList<GameAction>()
    val dests = listOf<FieldCard?>(null) + p.field
    for (dst in dests) {
        val toId = dst?.instanceId ?: "Reserve"
        if (toId == fromId) continue
        val toLabel = dst?.displayName ?: "R"

        // ソウルコアと通常コアの交換
        val dstNormal = if (toId == "Reserve") p.reserve.normal else dst!!.cores.normal
        if (dstNormal > 0) {
            out.add(GameAction(0, "MOVE_CORE", "🔄【コア交換】 $fromLabel ⇄ $toLabel",
                "🔶 ⇄ 🔷 を交換",
                targetInstanceId = dst?.instanceId,
                moves = listOf(
                    CoreMove(fromId, toId, fromLabel, toLabel, normal = 0, soul = 1),
                    CoreMove(toId, fromId, toLabel, fromLabel, normal = 1, soul = 0)
                ),
                rearrangeKey = "SOUL"))
        }
    }
    return out
}

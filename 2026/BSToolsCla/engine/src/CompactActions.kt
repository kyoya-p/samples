package bstools.model

/**
 * 学習・自動対戦向けの「圧縮した選択肢列挙」。
 *
 * [enumerateActions] はコアの供給元・配置数・移動数の組み合わせをすべて列挙するため、
 * メインステップでは1局面で数百手になり、1手先読み評価のコストが極端に大きい。
 *
 * ここでは「エンティティ単位の可否判定 + パラメータの既定値」という設計にする
 * (bs.quensonwai.com の controls / play / rearrange の分け方を参考にした再実装):
 *  - プレイ (召喚・配置・マジック): 支払いは既定ルールで1通りに決める
 *    (リザーブの通常コア優先、足りないときだけソウルコア)。配置コアはLvの境目
 *    (最低Lv・より上のLvに届く枚数) と、ソウルコアを乗せるかどうかだけを候補にする。
 *    リザーブだけで払えないときは従来の列挙から数手だけ拾う (フィールドのコアを使う合法手を消さない)
 *  - コア移動: 単発の移動の総当たりではなく「意味のある再配置」だけにする
 *    (次Lvへ上げる / 余剰コアをリザーブへ戻す / Lv1まで下げる / ソウルコアを載せ替える /
 *     余剰があるカードから他のカードのLvを上げる / ソウルコアと通常コアを入れ替える)
 *  - アタック・ブロック: もともとエンティティ単位なので従来の列挙をそのまま使う
 *
 * 適用は従来の [applyAction] と同じ GameAction 形式 (SUMMON/USE_MAGIC/MOVE_CORE) なので、
 * ルール処理そのものは一切変えない。選べる手の集合が従来列挙の部分集合になるだけ。
 */
fun enumerateCompactActions(state: GameState): List<GameAction> {
    if (state.winner != null) return emptyList()
    if (state.step != Step.MAIN && state.step != Step.MAIN_2) {
        return markRepetitions(state, enumerateRawActions(state))
    }
    val p = state.choosingPlayer
    val prefix = if (state.step == Step.MAIN_2) "[第2メイン] " else ""
    val acts = ArrayList<GameAction>()

    for (handIdx in p.hand.indices.distinctBy { p.hand[it].cardNo }) {
        acts.addAll(compactPlayOptions(p, p.hand[handIdx], handIdx, prefix))
    }
    acts.addAll(compactCoreMoves(p))

    val marked = markRepetitions(state, acts)
    return marked.mapIndexed { i, a -> a.copy(index = i + 1) }
}

private const val RESERVE = "Reserve"

private fun playCategory(card: CardData, prefix: String) = when (card.category) {
    CardCategory.MAGIC -> "✨【${prefix}マジック使用】 ${card.name}"
    CardCategory.NEXUS -> "🌐【${prefix}配置】 ${card.name}"
    else -> "🎴【${prefix}召喚】 ${card.name}"
}

private fun compactPlayOptions(p: PlayerState, card: CardData, handIdx: Int, prefix: String): List<GameAction> {
    val cost = summonCost(card, p)
    val category = playCategory(card, prefix)
    val actionType = if (card.category == CardCategory.MAGIC) "USE_MAGIC" else "SUMMON"

    // 既定の支払い: リザーブの通常コア → 足りなければソウルコア
    val payNormal = minOf(cost, p.reserve.normal)
    val paySoul = if (cost > payNormal && p.reserve.soul > 0) 1 else 0
    val restNormal = p.reserve.normal - payNormal
    val restSoul = p.reserve.soul - paySoul
    val minimum = requiredCoresOnSummon(card)
    val reserveOnly = payNormal + paySoul >= cost &&
        (card.category == CardCategory.MAGIC || restNormal + restSoul >= minimum)

    if (!reserveOnly) {
        // フィールドのコアを使わないと払えない: 従来の列挙から最小配置の数手だけ採用
        val sources = coreSources(p)
        val place = placementOptions(card, cost, sources).firstOrNull() ?: return emptyList()
        return playOptions(card, handIdx, category, actionType, cost, place, sources, limit = 4)
    }

    val payFrom = if (cost > 0) listOf(CoreAlloc(RESERVE, "R", payNormal, paySoul)) else emptyList()
    val payLabel = if (cost == 0) "コスト不要" else "コスト${coreIcons(payNormal, paySoul)}"
    // 末尾の括弧は WebUI がクリックで選んだコアと突き合わせるため、全列挙と同じ「場所:個数アイコン」の形式にする
    val payStr = if (cost > 0) "R:${coreIcons(payNormal, paySoul)}" else "なし"

    if (card.category == CardCategory.MAGIC) {
        return listOf(
            GameAction(0, actionType, category, "$payLabel, コアは置かない, (支払:$payStr, 配置:なし)",
                handIndex = handIdx, costToPay = cost, payFrom = payFrom)
        )
    }

    // 配置枚数の候補: 最低LvのLvコスト + より上のLvに届く枚数 (Lvの境目だけ)
    val totals = mutableSetOf(minimum)
    for ((i, req) in card.lvCosts.withIndex()) if (i > 0 && req > minimum) totals.add(req)

    val out = ArrayList<GameAction>()
    for (t in totals.sorted()) {
        // 通常コアのみ / ソウルコアを1個含める の2通り
        for (soul in 0..minOf(1, restSoul)) {
            val normal = t - soul
            if (normal < 0 || normal > restNormal) continue
            if (t == 0 && soul == 0 && out.any { it.placeFrom.isEmpty() }) continue
            val placeFrom = if (t > 0) listOf(CoreAlloc(RESERVE, "R", normal, soul)) else emptyList()
            val lv = levelFor(card.lvCosts, t, soul, card.shinkai)
            val placeLabel = if (t == 0) "配置コアなし" else "配置コア${coreIcons(normal, soul)}"
            val placeStr = if (t == 0) "なし" else "R:${coreIcons(normal, soul)}"
            out.add(
                GameAction(0, actionType, category,
                    "$payLabel, $placeLabel → Lv$lv, (支払:$payStr, 配置:$placeStr)",
                    handIndex = handIdx, costToPay = cost, payFrom = payFrom, placeFrom = placeFrom)
            )
        }
    }
    return out
}

private fun moveAction(p: PlayerState, label: String, moves: List<CoreMove>): GameAction =
    GameAction(0, "MOVE_CORE", "🔷【コア再配置】 $label",
        moves.joinToString(" / ") { "${it.fromLabel}➔${it.toLabel} ${coreIcons(it.normal, it.soul)}" },
        targetInstanceId = moves.first().toId, moves = moves)

/** 現在Lvを保ったまま外せるコア数 (現在LvのLvコストを超える分) */
private fun surplus(fc: FieldCard): Int {
    val lv = fc.level
    if (lv <= 0) return 0
    val req = fc.lvCosts.getOrNull(lv - 1) ?: return 0
    return maxOf(0, fc.cores.total - maxOf(req, if (fc.category == CardCategory.NEXUS) 0 else 1))
}

/** 次のLvに必要な追加コア数 (Lvコストが0=ソウルコア条件のLvは対象外)。最大Lvなら null */
private fun needForNextLevel(fc: FieldCard): Int? {
    val next = fc.lvCosts.getOrNull(fc.level) ?: return null
    if (next <= 0) return null
    return maxOf(0, next - fc.cores.total).takeIf { it > 0 }
}

private fun compactCoreMoves(p: PlayerState): List<GameAction> {
    val out = ArrayList<GameAction>()
    for (fc in p.field) {
        val name = fc.displayName
        // 1. リザーブから次Lvへ上げる
        needForNextLevel(fc)?.let { need ->
            val soul = if (p.reserve.normal < need && p.reserve.soul > 0) 1 else 0
            if (p.reserve.normal >= need - soul) {
                out.add(moveAction(p, "R ➔ $name (Lv${fc.level + 1}へ)",
                    listOf(CoreMove(RESERVE, fc.instanceId, "R", name, need - soul, soul))))
            }
            // 5. 余剰コアを持つ他のカードから次Lvへ上げる
            for (donor in p.field) {
                if (donor === fc || surplus(donor) < need) continue
                val dn = minOf(need, donor.cores.normal)
                if (dn < need) continue
                out.add(moveAction(p, "${donor.displayName} ➔ $name (Lv${fc.level + 1}へ)",
                    listOf(CoreMove(donor.instanceId, fc.instanceId, donor.displayName, name, dn, 0))))
            }
        }
        // 2. Lvを保ったまま余剰の通常コアをリザーブへ戻す
        val sp = minOf(surplus(fc), fc.cores.normal)
        if (sp > 0) {
            out.add(moveAction(p, "$name ➔ R (余剰)", listOf(CoreMove(fc.instanceId, RESERVE, name, "R", sp, 0))))
        }
        // 3. Lv1まで下げてコアを回収 (Lv2以上のとき)
        if (fc.level >= 2) {
            val keep = maxOf(1, fc.lvCosts.firstOrNull() ?: 1)
            val release = fc.cores.total - keep
            val relSoul = if (fc.cores.soul > 0 && fc.cores.normal < release) 1 else 0
            val relNormal = release - relSoul
            if (release > sp && relNormal <= fc.cores.normal) {
                out.add(moveAction(p, "$name ➔ R (Lv1へ)", listOf(CoreMove(fc.instanceId, RESERVE, name, "R", relNormal, relSoul))))
            }
        }
        // 4. ソウルコアの載せ替え (リザーブ⇄カード)
        if (p.reserve.soul > 0) {
            out.add(moveAction(p, "R ➔ $name (ソウルコア)", listOf(CoreMove(RESERVE, fc.instanceId, "R", name, 0, 1))))
        }
        if (fc.cores.soul > 0 && fc.category != CardCategory.NEXUS) {
            val after = levelFor(fc.lvCosts, fc.cores.total - 1, 0)
            if (after >= 1) out.add(moveAction(p, "$name ➔ R (ソウルコア)", listOf(CoreMove(fc.instanceId, RESERVE, name, "R", 0, 1))))
        }
    }
    out.addAll(soulSwaps(p))
    return out
}

/**
 * 6. ソウルコアと通常コアの入れ替え (総合ルール 7-6-1-2-2「フィールドやリザーブのソウルコアとコアの場所を交換」)。
 * ソウルコアがある場所 (リザーブかカード) と、通常コアがある他の各場所との1対1の交換を列挙する。
 * コアの数は変わらないので消滅は起きないが、《真界放》のLvや、ソウルコアを参照する効果に影響する。
 */
private fun soulSwaps(p: PlayerState): List<GameAction> {
    val soulAt: Pair<String, String> = when {
        p.reserve.soul > 0 -> RESERVE to "R"
        else -> p.field.firstOrNull { it.cores.soul > 0 }?.let { it.instanceId to it.displayName } ?: return emptyList()
    }
    val places = listOf(Triple(RESERVE, "R", p.reserve.normal)) + p.field.map { Triple(it.instanceId, it.displayName, it.cores.normal) }
    return places.filter { (id, _, normal) -> id != soulAt.first && normal > 0 }.map { (id, label, _) ->
        GameAction(0, "MOVE_CORE", "🔄【コア交換】 ${soulAt.second} ⇄ $label", "🔶 ⇄ 🔷 を交換",
            targetInstanceId = id,
            moves = listOf(
                CoreMove(soulAt.first, id, soulAt.second, label, normal = 0, soul = 1),
                CoreMove(id, soulAt.first, label, soulAt.second, normal = 1, soul = 0)
            ))
    }
}

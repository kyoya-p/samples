# Battle Spirits Rule Reference & Engine Implementation

## 1. Official Comprehensive Rules Compliance

### Maintain Cost (維持コスト - 総合ルール 3-2-5-4 / 3-3-3-3)
- **Spirits**: Lv1 cost is typically $\ge 1$. Placing 0 cores on a Spirit triggers immediate vanish (消滅 / 総合ルール 3-2-5-4).
- **Nexus**: Lv1 cost is $0$. A Nexus remains in the arena with 0 cores without vanishing (総合ルール 3-3-3-3).
- Engine implementation:
  ```kotlin
  val lv1Cost = fc.lvCosts.firstOrNull() ?: 0
  val vanish = afterTotal < lv1Cost
  ```

### No Artificial Core Cap on Summon Placement (召喚時配置コア上限の撤廃 / 総合ルール 11-1-1)
- Spirits/Nexuses can be placed with **any quantity of cores up to the available remaining cores** (`1..maxPlace`), without being capped by the card's maximum Lv cost (`maxCost`).
- Limiting to `minOf(maxCost, maxPlace)` is strictly prohibited; players may legitimately place excess cores onto a spirit to trigger self-destruction/vanish of other donor spirits or prepare for subsequent turns.
- Engine implementation:
  ```kotlin
  // Correct (LevelActions.kt & Rules.kt)
  val minimum = requiredCoresOnSummon(card)
  val thresholds = (minimum..maxPlace).toList()
  ```

### Soul Core Exchange (コア交換 - 総合ルール 11-2)
- **Core Move vs Core Swap**:
  - `MOVE_CORE`: Changes the total core count on cards and reserve. Removing all cores causes a spirit to vanish.
  - `SWAP_CORE` (`🔄【コア交換】`): Atomic 1-to-1 exchange between Soul Core (🔶/🔴) and Normal Core (🔷). Total cores on both donor and receiver remain unchanged.
  - **No Vanish Invariant**: Since core counts do not change, Core Swap never causes vanishing (`vanishing_card_ids = emptyList()`). This allows safely transferring the Soul Core away from a 1-core spirit without destroying it.
- Engine implementation (`CompactActions.kt`):
  ```kotlin
  val places = listOf(Triple(RESERVE, "R", p.reserve.normal)) + p.field.map { Triple(it.instanceId, it.displayName, it.cores.normal) }
  places.filter { (id, _, normal) -> id != soulAt.first && normal > 0 }.map { (id, label, _) ->
      GameAction(0, "MOVE_CORE", "🔄【コア交換】 ${soulAt.second} ⇄ $label", "🔶 ⇄ 🔷 を交換",
          targetInstanceId = id,
          moves = listOf(
              CoreMove(soulAt.first, id, soulAt.second, label, normal = 0, soul = 1),
              CoreMove(id, soulAt.first, label, soulAt.second, normal = 1, soul = 0)
          ))
  }
  ```

### Step Start Effects & Simultaneous Trigger Resolution (ステップ開始時効果・同時発揮 / 総合ルール 8-1)
- Cards with "At the start of step" triggers (e.g. `[Lv2]『自分のメインステップ開始時』...`) are enumerated via `CardScript.enumerateStepStartActions`.
- **Preemption Rule**: When pending step-start effects exist, normal step actions (Summon, Core Move, End Step) are blocked, and only the pending trigger actions are enumerated.
- **Simultaneous Resolution (8-1)**: When multiple effects trigger at the same time, the turn player chooses which effect to resolve first. Each resolution consumes one trigger (recorded in `state.usedOncePerTurnEffects`), and remaining unconsumed triggers remain enumerated until all are resolved.

### Multiple-Choice Target Resolution on Triggers (召喚時・誘発効果の複数対象選択原則 / 総合ルール 8-1 / 10-1)
- When an on-summon or trigger effect (e.g. すくいーど: 相手の疲労状態のスピリット1体を重疲労させる) has multiple legal targets, the engine must enumerate an individual action for **each valid target**, rather than picking an arbitrary default.

### Two Flash Timings per Battle & Priority (2回のフラッシュタイミング / 総合ルール 8-1-2 & 8-1-4)
Battle sequence proceeds in 5 distinct phases:
1. **Attack Declaration (8-1-1)**: Turn player exhausts a spirit to attack; triggers attack effects.
2. **Flash Timing 1 (Attack Flash / 8-1-2)**:
   - Priority starts with the **defending player** (8-1-2-1).
   - "End Step" is strictly prohibited; only flash actions or pass (`PASS_FLASH`) are valid.
   - Ends only after **two consecutive passes** from alternating players, transitioning to Block Declaration.
3. **Block Declaration (8-1-3)**:
   - Defending player exhausts an eligible spirit to block, or passes to take life damage.
   - Triggers block effects.
4. **Flash Timing 2 (Block Flash / 8-1-4)**:
   - **8-1-4-1**: When a block is declared, a **second Flash Timing** occurs before battle resolution.
   - Priority again starts with the **defending player** (8-1-4-1).
   - Alternates until **two consecutive passes**, after which the battle advances to Battle Resolution (BP comparison).
   - *(Note: If block was skipped/passed in 8-1-3, Flash Timing 2 does not occur and the battle immediately resolves life damage)*.
5. **Battle Resolution (8-1-5 & 8-1-6)**:
   - If blocked: Spirits compare BP; lower BP is destroyed (ties destroy both).
   - If unblocked: Defending player loses life equal to attacking spirit's symbols.

### Core Movement Combinatorics
- For any donor with $n$ normal cores and $s$ soul cores:
  - If $n > 0$, moves for $1 \dots n$ normal cores are created.
  - If $s > 0$, single soul core move (0 normal, 1 soul) is created.
  - If $s > 0$ and $n > 0$, mixed moves for $k$ normal + 1 soul ($k \in 1 \dots n$) are created.

### Loop Prevention via CoreWindow
- Instead of tracking symmetric pairs (`A <-> B`), the engine tracks directed transitions: `"${from}->${to}"`.
- Sequential forward moves (e.g. moving 1 core from R to card A, then another core from R to card A) are permitted.
- Reversing the move (card A back to R) in the same core window is blocked.

## 2. Official Rule Compliance Audit (2026-11-08)

Sources: [ルールマニュアル［スタンダード］](https://www.battlespirits.com/rule/official-rule-manual-standard/) (page03–12), [バトスピWiki ルール](https://batspi.com/index.php?%E3%83%AB%E3%83%BC%E3%83%AB) (基本ルール / ターンの流れ / わかりづらいルール / フォーマット / 用語集; mostly 2020 Eternal-era text, official manual takes precedence).

### Known Gaps (未修正)
| # | Gap | Location | Rule |
|---|-----|----------|------|
| A | Attacker leaving during Flash 1 still goes to Block Declaration; PASS_BLOCK deals 1 life | `Rules.kt` advanceStep / PASS_BLOCK | Battle ends, no life damage |
| B | `onDestroy` is never called | `Rules.kt` destroySpirit | 破壊時 effects resolve before trash |
| 1 | Flash magic/effects enumerated during `BLOCK_DECLARATION` | `Rules.kt` enumerateRawActions | No flash in block declaration |
| 2 | `maxOf(1, symbols.size)` life loss | `Rules.kt` ATTACK detail / BLOCK_DECLARATION / PASS_BLOCK | Loss = symbol count |
| 3 | No mulligan; P1 always first | `GameSetup.kt` | One mulligan each |
| 4 | No deck validation | `CardLoader.loadDeck` | ≥40, ≤3 same name, block icon A |
| 6 | Blocker's `onBattleEnd` not called; battle-only effects not cleared | `Rules.kt` resolveBattle / PASS_BLOCK | Both sides' バトル終了時 |
| 7 | Flash magic detected by `effect.contains("フラッシュ")` | `Rules.kt` | Use structured timing data |
| 8 | No 待機状態 / 消滅時 effects | `resolveVanish` / `destroySpirit` | 待機状態 before trash |
| 9 | Magic effects unimplemented (cost only) | `USE_MAGIC` | — |
| D | `END_STEP` offered while `pendingEffect` / step-start effects exist | `server/src/Evaluation.kt` stepEndAction | Mandatory effects must resolve |

### Verified Compliant
- Initial state (life 5, reserve 3+1 soul, hand 4); 8-step Standard / 7-step Eternal; turn-1 skips Core/Attack/Main2 but draws.
- Mandatory reduction, payment from reserve/field, paid cores → trash; placement ≥ min Lv; Nexus may have 0 cores.
- Destroyed/vanished card cores → reserve; life cores → reserve.
- Flash priority defender-first, alternating, ends on two consecutive passes; Flash 2 only if blocked.
- BP lower destroyed, tie both; block negates life damage even if blocker leaves.
- 重疲労 recovers one stage; 真界放 Lv2 by soul core; deck-0 loss checked at Start Step.

### Skill vs Code Discrepancies
- SKILL §1-3 names `SWAP_CORE`, but code emits `MOVE_CORE` with category `🔄【コア交換】`.
- SKILL §1-7 says coreWindow removed, §1 "Loop Prevention" says directed `from->to` keys; code uses `GameAction.rearrangeKey` (instanceId / `SOUL` / `ALL`) in `GameState.coreWindow`.

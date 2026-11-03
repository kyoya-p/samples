# Battle Spirits Rule Reference & Engine Implementation

## 1. Official Comprehensive Rules Compliance

### Maintain Cost (維持コスト)
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

### Step Start Effects & Simultaneous Trigger Resolution (ステップ開始時効果・同時発揮 / 総合ルール 8-1)
- Cards with "At the start of step" triggers (e.g. `[Lv2]『自分のメインステップ開始時』...`) are enumerated via `CardScript.enumerateStepStartActions`.
- **Preemption Rule**: When pending step-start effects exist, normal step actions (Summon, Core Move, End Step) are blocked, and only the pending trigger actions are enumerated.
- **Simultaneous Resolution (8-1)**: When multiple effects trigger at the same time, the turn player chooses which effect to resolve first. Each resolution consumes one trigger (recorded in `state.usedOncePerTurnEffects`), and remaining unconsumed triggers remain enumerated until all are resolved.

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

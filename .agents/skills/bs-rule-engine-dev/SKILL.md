---
name: bs-rule-engine-dev
description: >-
  Comprehensive engineering, testing, and UI debugging runbook for the Battle Spirits
  Rule Engine (BSRuleEngineServer). Use this skill when modifying, debugging, or verifying
  Battle Spirits card logic, core movement and allocation, maintaining official rules,
  running test suites, or updating the Web Playmat interface.
---

# Battle Spirits Rule Engine & UI Development Skill

This skill provides the architecture reference, official rule compliance invariants, and Web UI integration patterns for `2026/BSRuleEngineServer`.

---

## 1. Core Rule Invariants (Battle Spirits Comprehensive Rules)

### Core Movement & Level Invariants
1. **Nexus Maintain Cost (維持コスト - 総合ルール 3-3-3-3)**:
   - Nexus cards have a Lv1 maintain cost of 0 (`lvCosts = listOf(0, ...)`).
   - A Nexus with 0 cores does **NOT** vanish (消滅). Never hardcode category checks; always evaluate `lvCosts.firstOrNull() ?: 0`.
   - Spirits require at least 1 core (or their specified Lv1 cost); reducing them to 0 cores causes immediate vanish (`vanish = true`).

2. **No Artificial Core Cap on Summon Placement (召喚時配置コア上限の撤廃 / 総合ルール 11-1-1)**:
   - Placement options for summoned spirits must include all counts up to total remaining cores (`(minimum..maxPlace).toList()`).
   - Never cap placements at `maxCost` (e.g. `minOf(maxCost, maxPlace)`), allowing strategic self-destruction of field spirits or core stockpiling.

3. **Soul Core Exchange (コア交換 - 総合ルール 11-2)**:
   - **Core Move vs Core Swap**:
     - `MOVE_CORE`: Changes total core count on donor and receiver. Removing all cores causes a spirit to vanish.
     - `SWAP_CORE` (`🔄【コア交換】`): Atomic 1-to-1 exchange between Soul Core (🔶/🔴) and Normal Core (🔷). Total cores on both cards remain unchanged.
     - **No Vanish Invariant**: Since core counts do not change, Core Swap never causes vanishing (`vanishing_card_ids = emptyList()`). This allows safely transferring the Soul Core away from a 1-core spirit without destroying it.

4. **Multiple-Choice Target Resolution on Triggers (召喚時・誘発効果の複数対象選択原則 / 総合ルール 8-1 / 10-1)**:
   - When an on-summon or trigger effect (e.g. すくいーど: 相手の疲労状態のスピリット1体を重疲労させる) has multiple legal targets, the engine must enumerate an individual action for **each valid target**, rather than picking an arbitrary single default.

5. **Step Start Effects & Simultaneous Resolution (ステップ開始時効果・同時発揮 / 総合ルール 8-1)**:
   - Cards with step-start triggers (e.g. `[Lv2]『自分のメインステップ開始時』...`) implement `enumerateStepStartActions` / `onStepStartAction`.
   - When pending step-start effects exist, normal step actions (Summon, Core Move, End Step) are blocked.
   - If multiple effects trigger simultaneously, the player selects the resolution order one by one until all are resolved.

6. **Two Flash Timings per Battle & Priority (2回のフラッシュタイミング / 総合ルール 8-1-2 & 8-1-4)**:
   - **Flash 1 (After Attack / 8-1-2)**: Priority begins with defending player. 2 consecutive passes advance to Block Declaration. "End Step" is strictly illegal.
   - **Flash 2 (After Block / 8-1-4)**: If a block is declared, a **second Flash Timing occurs** before BP comparison. Priority again starts with defending player (8-1-4-1). 2 consecutive passes advance to Battle Resolution.
   - *(If block was passed/skipped in Block Declaration, Flash 2 is omitted and battle proceeds directly to life damage)*.

7. **Core Movement Enumeration**:
   - In the Main Step, players can freely move cores between Field cards and Reserve as many times as desired without artificial turn/window restrictions (`coreWindow` restrictions removed).
   - All core movements (Normal core, Soul core, and mixed) are unified under the category `🔷【コア移動】 移動元 ➔ 移動先`.
   - For any donor holding $N$ normal cores and $S$ soul cores ($S \in \{0, 1\}$):
     - Normal core moves: Enumerate every quantity $k \in 1 \dots N$.
     - Soul core moves: Enumerate $S=1$ alone ($0$ normal + $1$ soul) and mixed moves ($k$ normal + $1$ soul for $k \in 1 \dots N$).

---

## 2. Server API & Data Contract

Server endpoint: `http://localhost:8090` (Room parameter: `?sid=<room_id>` or `?room=<room_id>`).

### State JSON Structure (`/api/state`)
- **Phase field**: `data.phase` contains strings like `"メインステップ [スタンダード]"`.
  > [!WARNING]
  > Never use `data.step` or `data.phase === "MAIN"`. Always test with `data.phase.includes("メインステップ")`.
- **Action Groups**: Available actions are grouped in `data.groups` (NOT `data.actions`).
  - Each group has `category`, `options`, `best_eval`, `all_forbidden`, `target_instance_id`.
  - Each option inside `g.options` has:
    - `index`: Global action index for `/api/step?sid=...&act=<index>` (or `POST /api/act`)
    - `detail`: Human-readable description
    - `core_effects`: Array of `{ source_id, label, normal, soul, direction: "from" | "to" }`
    - `vanishing_card_ids`: Array of card IDs that will vanish upon executing this action
    - `repeats_position`: Boolean indicating return to previously visited state

---

## 3. Web UI Architecture (`server/web/playmat2.html` & `index.html`)

### Core Interaction Workflows
1. **Direct Mouse Move & Safe-First Core Swap (コア直接移動とコア交換の安全優先マッチング原則)**:
   - Click source core (Reserve `Reserve` or Field Card `fc_...`): Highlights source (`selectedCoreMoveSource`).
   - Click destination card or Reserve: Triggers `executeDirectCoreMove(fromId, toId, normalCount, soulCount)`.
   - **Safe-First Matching Invariant**:
     - Candidates with `!a.vanishing_card_ids || a.vanishing_card_ids.length === 0` are strictly preferred over vanishing moves.
     - When `soulCount > 0`, `🔄【コア交換】` actions are prioritized above ordinary moves. This guarantees that moving the soul core off a 1-core spirit safely swaps for a normal core instead of destroying the spirit.

2. **Drag-and-Drop Pending Summon & Allocation UI (仮召喚ドラッグ＆コア移動確定仕様)**:
   - 手札からフィールドへドラッグ＆ドロップすると、フィールド上に**半透明の仮召喚カード（`.pending-card`）**が出現する。
   - リザーブや仮召喚カード上のコア、またはHUDの増減ボタン（`➖`, `➕`, `🔴ソウルコア切替`）を操作して、支払いコストと配置コアをグラフィカルに移動・決定できる。
   - **Donor Net Contribution & Strict Vanishing Guard (`syncPendingSummonWithAllocatedPayment`)**:
     - サーバーの `moves` 順序（配置移動 ➔ 支払移動）に依存せず、各供出元の純拠出数（`allocatedPayDonors` + `allocatedPlaceDonors`）で完全照合する。
     - **厳格消滅防壁 (Strict Vanishing Guard)**: ユーザーが0個までコアを使い切っていないスピリットを消滅させる選択肢（`option.vanishing_card_ids` に残コア $> 0$ のカードが含まれる手）は **100% 厳格に除外** される。
   - **すべて充足したら召喚ボタンを有効化原則 (`isPendingBothComplete`)**:
     - 支払コストが充足（`allocatedPayDonors.length === requiredPayCost`）かつ配置／維持コアが充足（`allocatedPlaceDonors.length >= requiredPlaceCores`）した瞬間、ステップ番号（Step 1/Step 2）に関わらず即座に召喚確定ボタン（`#btn-pending-confirm`）が有効化（`✅ 召喚確定 (Space)`）される。
     - ユーザーは不要な「次へ」ボタンのクリックを挟むことなく、Spaceキーまたはボタン押下で即時召喚できる。
     - 未充足時は理由を明記して無効化（`⚠️ コスト不足 (あと N 個)` / `⚠️ 維持コア未指定 (あと N 個)`）。
   - **点滅枠ハイライトと冗長ラベル文字抑制の原則**:
     - トラッシュの支払コスト枠（`.trash-pending-pay-enclosure`）および仮召喚カードの維持／配置コア枠（`.card-pending-place-enclosure`）は、未充足時はオレンジ破線枠で滑らかに点滅（`blink-orange-pay-enclosure`）してユーザーの注意を促す。
     - 充足した瞬間に点滅を停止（`animation: none;`）し、落ち着いたグリーンの完了枠に切り替わる。
     - 「コスト支払い中」「維持コア指定中」などのカードや盤面を覆い隠す冗長なテキストラベルは非表示（`display: none !important;`）とし、クリーンな表示を徹底する。

3. **Card States & Rotation Invariants (疲労・重疲労時のステータスバッジ水平維持原則)**:
   - カードが90°回転（疲労 / `.exhausted`）または180°回転（重疲労 / `.heavy-exhausted`）しても、Lvバッジ（`.card-badge-lv`）およびBPバッジ（`.card-badge-bp`）は逆回転（`transform: rotate(-90deg)` / `rotate(-180deg)`)を適用し、常に水平（0° 正立）で視認可能に保つ。
   - バッジの位置調整（`top`/`bottom`/`right`/`left`）は、回転後のカード四隅の内側にぴったり収まり、カード外へはみ出さないように配置する。

4. **Action List Rendering (`renderGroups`)**:
   - Core moves must **NOT** be hidden inside a closed accordion when there are only a few choices ($\le 5$).
   - They appear directly in the right sidebar for immediate visibility and execution.

5. **UI Duplication & Clutter Suppression Invariants (無駄なUI・重複ボタンの抑制原則)**:
   - **Single Source of Control**: When a system operation is already accessible from persistent global toolbars (e.g., `#btn-undo` / `◀ 戻る`, `新規ゲーム開始`, `同じ条件でリスタート`), **never duplicate** those action buttons inside transient or notification banners (such as the victory `#winner` banner).
   - Notification banners must strictly present status information (e.g. `🏆 ゲーム終了: プレイヤーX の勝利！`) and avoid cluttering the interface with redundant action buttons.

6. **Strict Instance ID Matching Invariant (カード名判定の禁止・インスタンスID照合原則)**:
   - **Never match or look up field cards by card name (`card.name` / `rawName`)** for game actions (Attack declaration, Block declaration, Attacker identification, Level/Core actions).
   - Server action categories include sequence numbers (e.g., `🗡️【アタック】 #5 パッファー`), while `card.name` on the client is `"パッファー"`, and multiple copies of the same card can exist on the field simultaneously.
   - Always bind and resolve field card actions strictly by unique instance ID (`g.target_instance_id === o.id`, `data.attacking_card_id === o.id`).
   - For hand card actions, always match by `g.card_no === c.card_no` (never substring matching on card names).

7. **Hand Card Summon Accordion & Option Hierarchy (手札アコーディオン2段階グルーピング原則)**:
   - **Vanish Label Separation**: Vanish notices (`⚠️...消滅`) must be stripped from the `costPlacement` grouping key so that different vanishing donors do not fracture the options into dozens of fragmented rows.
   - **Soul Core Count Normalization (`normalizeCostPlacement`)**: Both soul cores and normal cores in cost/placement are normalized to total core counts, guaranteeing that hand card menus cleanly collapse into rows by placement count (e.g. 1, 2, 3 cores).
   - **Placement Soul Core Separation Invariant**: Always separate `配置コア 1個 [🔷] (通常コア)` and `配置コア 1個 [🔴] (ソウルコア)` into distinct sub-headers.

8. **Vanish Detection Preview Invariant (`getVanishingCardIds`)**:
   - When the server provides `option.vanishing_card_ids` (even if empty `[]`), always trust and return it directly. Never guard with `length > 0` which falsely triggers client fallback logic.
   - In fallback calculations, explicitly exempt cards where `category === "NEXUS"` or `lv_costs[0] === 0` from vanishing.

9. **Hot-Reloading Static Web Assets & Multi-Playmat Routing**:
   - `loadHtmlFile(filename)` reads directly from disk on every request without keeping an in-memory cache, enabling immediate UI updates upon browser reload (F5) without restarting the daemon.
   - **Playmat 1 (`index.html` at `/`)**: Dense textual analysis interface designed for deep minimax search and action validation.
   - **Playmat 2 (`playmat2.html` at `/playmat2`)**: Graphical authentic TCG physical card playmat interface.

---

## 4. Verification & Testing

Run the automated rule compliance test suite:
```bash
# In samples root or 2026/BSRuleEngineServer
mise run test
```

E2E Playwright tests (in `2026/BSRuleEngineServer/tests/e2e`):
```bash
npx playwright test
```
Detailed documentation is available in [Core Rules Reference](./references/core_rules.md) and [UI Integration Reference](./references/ui_integration.md).

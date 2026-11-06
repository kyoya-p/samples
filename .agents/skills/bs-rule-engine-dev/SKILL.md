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
1. **Nexus Maintain Cost (維持コスト)**:
   - Nexus cards have a Lv1 maintain cost of 0 (`lvCosts = listOf(0, ...)`).
   - A Nexus with 0 cores does **NOT** vanish (消滅). Never hardcode category checks; always evaluate `lvCosts.firstOrNull() ?: 0`.
   - Spirits require at least 1 core (or their specified Lv1 cost); reducing them to 0 cores causes immediate vanish (`vanish = true`).

2. **No Artificial Core Cap on Summon Placement (召喚時配置コア上限の撤廃 / 総合ルール 11-1-1)**:
   - Placement options for summoned spirits must include all counts up to total remaining cores (`(minimum..maxPlace).toList()`).
   - Never cap placements at `maxCost` (e.g. `minOf(maxCost, maxPlace)`), allowing strategic self-destruction of field spirits or core stockpiling.

3. **Step Start Effects & Simultaneous Resolution (ステップ開始時効果・同時発揮 / 総合ルール 8-1)**:
   - Cards with step-start triggers (e.g. `[Lv2]『自分のメインステップ開始時』...`) implement `enumerateStepStartActions` / `onStepStartAction`.
   - When pending step-start effects exist, normal step actions (Summon, Core Move, End Step) are blocked.
   - If multiple effects trigger simultaneously, the player selects the resolution order one by one until all are resolved.

4. **Two Flash Timings per Battle & Priority (2回のフラッシュタイミング / 総合ルール 8-1-2 & 8-1-4)**:
   - **Flash 1 (After Attack / 8-1-2)**: Priority begins with defending player. 2 consecutive passes advance to Block Declaration. "End Step" is strictly illegal.
   - **Flash 2 (After Block / 8-1-4)**: If a block is declared, a **second Flash Timing occurs** before BP comparison. Priority again starts with defending player (8-1-4-1). 2 consecutive passes advance to Battle Resolution.
   - *(If block was passed/skipped in Block Declaration, Flash 2 is omitted and battle proceeds directly to life damage)*.

5. **Core Movement Enumeration**:
   - In the Main Step, players can freely move cores between Field cards and Reserve as many times as desired without artificial turn/window restrictions (`coreWindow` restrictions removed).
   - All core movements (Normal core, Soul core, and mixed) are unified under the category `🔷【コア移動】 移動元 ➔ 移動先` (do not create separate `【ソウルコア移動】` categories).
   - For any donor holding $N$ normal cores and $S$ soul cores ($S \in \{0, 1\}$):
     - Normal core moves: Enumerate every quantity $k \in 1 \dots N$.
     - Soul core moves: Enumerate $S=1$ alone ($0$ normal + $1$ soul) and mixed moves ($k$ normal + $1$ soul for $k \in 1 \dots N$).

6. **Soul Core Exchange (コア交換)**:
   - When one location has Soul Core and another has Normal Core, they can exchange without altering total core counts.

---

## 2. Server API & Data Contract

Server endpoint: `http://localhost:8090` (Room parameter: `?sid=<room_id>`).

### State JSON Structure (`/api/state`)
- **Phase field**: `data.phase` contains strings like `"メインステップ [スタンダード]"`.
  > [!WARNING]
  > Never use `data.step` or `data.phase === "MAIN"`. Always test with `data.phase.includes("メインステップ")`.
- **Action Groups**: Available actions are grouped in `data.groups` (NOT `data.actions`).
  - Each group has `category`, `options`, `best_eval`, `all_forbidden`, `target_instance_id`.
  - Each option inside `g.options` has:
    - `index`: Global action index for `/api/step?sid=...&act=<index>`
    - `detail`: Human-readable description
    - `core_effects`: Array of `{ source_id, label, normal, soul, direction: "from" | "to" }`
    - `repeats_position`: Boolean indicating return to previously visited state

---

## 3. Web UI Architecture (`server/web/index.html`)

### Core Interaction Workflows
1. **Direct Mouse Move (コア直接移動)**:
   - Click source core (Reserve `Reserve` or Field Card `fc_...`): Highlights source (`selectedCoreMoveSource`).
   - Click destination card or Reserve: Triggers `executeDirectCoreMove(fromId, toId, coreType)`.
   - Matches candidate action from `current.groups.flatMap(g => g.options)` where `fromEff.source_id === fromId` and `toEff.source_id === toId`.

2. **Action List Rendering (`renderGroups`)**:
   - Core moves must **NOT** be hidden inside a closed accordion when there are only a few choices ($\le 5$).
   - They appear directly in the right sidebar for immediate visibility and execution.

3. **Multi-Click Quantity Selector**:
   - For groups with multiple quantity options, clicking the header increments the target quantity (`⚡[N回]`), and automatically submits after a 450ms debounce window.

4. **UI Duplication & Clutter Suppression Invariants (無駄なUI・重複ボタンの抑制原則)**:
   - **Single Source of Control**: When a system operation is already accessible from persistent global toolbars (e.g., `#btn-undo` / `◀ 戻る`, `新規ゲーム開始`, `同じ条件でリスタート`), **never duplicate** those action buttons inside transient or notification banners (such as the victory `#winner` banner).
   - Notification banners must strictly present status information (e.g. `🏆 ゲーム終了: プレイヤーX の勝利！`) and avoid cluttering the interface with redundant action buttons.
   - Users interact exclusively via the fixed header controls to maintain consistent, clean, and intuitive UX.

5. **Strict Instance ID Matching Invariant (カード名判定の禁止・インスタンスID照合原則)**:
   - **Never match or look up field cards by card name (`card.name` / `rawName`)** for game actions (Attack declaration, Block declaration, Attacker identification, Level/Core actions).
   - Server action categories include sequence numbers (e.g., `🗡️【アタック】 #5 パッファー`), while `card.name` on the client is `"パッファー"`, and multiple copies of the same card can exist on the field simultaneously.
   - Always bind and resolve field card actions strictly by unique instance ID (`g.target_instance_id === o.id`, `data.attacking_card_id === o.id`).
   - For hand card actions, always match by `g.card_no === c.card_no` (never substring matching on card names).

6. **Hand Card Summon Accordion & Option Hierarchy (手札アコーディオン2段階グルーピング原則)**:
   - **Vanish Label Separation**: Vanish notices (`⚠️...消滅`) must be stripped from the `costPlacement` grouping key so that different vanishing donors do not fracture the options into dozens of fragmented rows.
   - **Soul Core Count Normalization (`normalizeCostPlacement`)**: Both soul cores and normal cores in cost/placement are normalized to total core counts, guaranteeing that hand card menus cleanly collapse into rows by placement count (e.g. 1, 2, 3 cores).
   - **Placement Soul Core Separation Invariant (配置ソウルコアの独立グルーピング原則)**:
     - 召喚・配置時のサブリスト階層化において、**配置するコアにソウルコアが含まれるか否か（通常コア配置 🔷 vs ソウルコア配置 🔴）は本質的な戦略差異**であるため、単なる個数（1個）だけで1つのサブリストに合算してはならない。
     - 必ず `配置コア 1個 [🔷] (通常コア)` と `配置コア 1個 [🔴] (ソウルコア)` のように独立したサブリストとして分離し、ボタン側でも `➔ [配置] 🔷` / `➔ [配置] 🔴` を明記すること。
   - **Drag-and-Drop Pending Summon & Allocation UI (仮召喚ドラッグ＆コア移動確定仕様)**:
     - 手札からフィールドへドラッグ＆ドロップすると、フィールド上に**半透明の仮召喚カード（`.pending-card`）**が出現する。
     - リザーブや仮召喚カード上のコア、またはHUDの増減ボタン（`➖`, `➕`, `🔴ソウルコア切替`）を操作して、支払いコストと配置コアをグラフィカルに移動・決定できる。
     - **すべて充足したら召喚ボタンを有効化原則 (`isPendingBothComplete`)**:
       - 支払コストが充足（`allocatedPayDonors.length === requiredPayCost`）かつ配置／維持コアが充足（`allocatedPlaceDonors.length >= requiredPlaceCores`）した瞬間、ステップ番号（Step 1/Step 2）に関わらず即座に召喚確定ボタン（`#btn-pending-confirm`）が有効化（`✅ 召喚確定 (Space)`）される。
       - ユーザーは不要な「次へ」ボタンのクリックを挟むことなく、Spaceキーまたはボタン押下で即時召喚できる。
       - 未充足時は理由を明記して無効化（`⚠️ コスト不足 (あと N 個)` / `⚠️ 維持コア未指定 (あと N 個)`）。
     - **点滅枠ハイライトと冗長ラベル文字抑制の原則**:
       - トラッシュの支払コスト枠（`.trash-pending-pay-enclosure`）および仮召喚カードの維持／配置コア枠（`.card-pending-place-enclosure`）は、未充足時はオレンジ破線枠で滑らかに点滅（`blink-orange-pay-enclosure`）してユーザーの注意を促す。
       - 充足した瞬間に点滅を停止（`animation: none;`）し、落ち着いたグリーンの完了枠に切り替わる。
       - 「コスト支払い中」「維持コア指定中」などのカードや盤面を覆い隠す冗長なテキストラベルは非表示（`display: none !important;`）とし、クリーンな表示を徹底する。
     - `✅ 確定`（またはSpaceキー）で正式召喚、`✖ 手札に戻す`（またはEscapeキー/画面外クリック）で安全にキャンセル可能。
   - Detailed donor combinations, vanishing warnings, and soul core placement are displayed clearly inside the opened second-tier buttons.

7. **Card States & Rotation Invariants (疲労・重疲労時のステータスバッジ水平維持原則)**:
   - カードが90°回転（疲労 / `.exhausted`）または180°回転（重疲労 / `.heavy-exhausted`）しても、Lvバッジ（`.card-badge-lv`）およびBPバッジ（`.card-badge-bp`）は逆回転（`transform: rotate(-90deg)` / `rotate(-180deg)`）を適用し、常に水平（0° 正立）で視認可能に保つ。
   - バッジの位置調整（`top`/`bottom`/`right`/`left`）は、回転後のカード四隅の内側にぴったり収まり、カード外へはみ出さないように配置する。

8. **Multiple-Choice On-Summon & Trigger Effects (召喚時・誘発効果の複数対象選択原則)**:
   - すくいーど等の召喚時効果で複数の対象（例：疲労状態の相手スピリット重疲労）が存在する場合、システムは単一の固定選択ではなく、対象ごとの個別選択肢を列挙してプレイヤーに選択させる。

9. **Vanish Detection Preview Invariant (`getVanishingCardIds`)**:
   - When the server provides `option.vanishing_card_ids` (even if empty `[]`), always trust and return it directly. Never guard with `length > 0` which falsely triggers client fallback logic.
   - In fallback calculations, explicitly exempt cards where `category === "NEXUS"` or `lv_costs[0] === 0` from vanishing.

10. **Hot-Reloading Static Web Assets & Multi-Playmat Routing**:
   - `loadHtmlFile(filename)` reads directly from disk on every request without keeping an in-memory cache, enabling immediate UI updates upon browser reload (F5) without restarting the daemon.
   - **Playmat 1 (`index.html` at `/`)**: Dense textual analysis interface designed for deep minimax search and action validation.
   - **Playmat 2 (`playmat2.html` at `/playmat2`)**: Graphical authentic TCG physical card playmat interface.
     - **Cards as Physical Objects**: Actual card images (`image_url`), smooth 90° rotation on exhaustion, attacking aura, and block highlighting.
     - **3D Deck Stack**: 3D layered card pile displaying official Battle Spirits card back with live count badge.
     - **Trash Zone**: Displays the top card face-up with card artwork and count badge; clicking opens a full-card gallery modal.
     - **Glowing Core Gems**: 3D crystal gems (🔷 Normal / 🔮 Soul) placed directly on field cards, life slots, and reserve trays.
     - **Clash Zone (激突エリア)**: Central confrontation arena displaying attacking and blocking spirits face-to-face with BP comparison.
     - **Card Inspector**: Live high-resolution card artwork and details on hover/click.
     - **One-Click Switcher**: Seamless switching between Playmat 1 and Playmat 2 via top toolbar buttons.
     - **Hand Drag & Drop Summon / Deployment**: Dragging a hand card to the field filters the action list exclusively for that card's summon / nexus placement options (allowing the user to choose their preferred core payment and placement allocation).
     - **Hand Card Click Effect Filtering**: Simply clicking a hand card filters and displays hand-activatable effects only (e.g. `USE_MAGIC` for magic cards, `FLASH_SUMMON` for Flatty's flash summon during opponent attack step, hand triggers), keeping the popover and action panel clean and context-focused.

---

## 4. Verification & Testing

Run the automated rule compliance test suite:
```bash
# In samples root or 2026/BSRuleEngineServer
mise run test
```
Detailed documentation is available in [Core Rules Reference](./references/core_rules.md) and [UI Integration Reference](./references/ui_integration.md).

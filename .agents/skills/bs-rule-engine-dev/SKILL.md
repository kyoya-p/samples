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

2. **Core Movement Enumeration**:
   - In the Main Step, players can freely move cores between Field cards and Reserve as many times as desired without artificial turn/window restrictions (`coreWindow` restrictions removed).
   - All core movements (Normal core, Soul core, and mixed) are unified under the category `🔷【コア移動】 移動元 ➔ 移動先` (do not create separate `【ソウルコア移動】` categories).
   - For any donor holding $N$ normal cores and $S$ soul cores ($S \in \{0, 1\}$):
     - Normal core moves: Enumerate every quantity $k \in 1 \dots N$.
     - Soul core moves: Enumerate $S=1$ alone ($0$ normal + $1$ soul) and mixed moves ($k$ normal + $1$ soul for $k \in 1 \dots N$).

3. **Soul Core Exchange (コア交換)**:
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

---

## 4. Verification & Testing

Run the automated rule compliance test suite:
```bash
# In samples root or 2026/BSRuleEngineServer
mise run test
```
Detailed documentation is available in [Core Rules Reference](./references/core_rules.md) and [UI Integration Reference](./references/ui_integration.md).

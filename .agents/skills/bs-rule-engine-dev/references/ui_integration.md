# Web Playmat UI Integration Reference

## 1. Direct Mouse Core Movement & Safe-First Core Swap

### Event Flow
1. **User clicks on source core**:
   - Reserve click: `handleSourceCoreClick("Reserve", "soul" | "normal")`
   - Field card core click: `handleSourceCoreClick(card.id, "soul" | "normal")`
   - Sets `selectedCoreMoveSource = { sourceId, coreType, label, normalCount, soulCount }` and highlights source element.
2. **User clicks on target entity**:
   - Target card click: invokes `executeDirectCoreMove(fromId, target.id, normalCount, soulCount)`
   - Target reserve click: invokes `executeDirectCoreMove(fromId, "Reserve", normalCount, soulCount)`
3. **Safe-First Matching & Core Swap Priority Invariant (安全優先マッチング原則)**:
   - When matching candidate actions from `current.groups.flatMap(g => g.options)`:
     - **Non-Vanishing Priority**: Actions where `!a.vanishing_card_ids || a.vanishing_card_ids.length === 0` are strictly prioritized over vanishing actions. Accidental spirit destruction must never happen via click-to-move.
     - **Core Swap Priority (`soulCount > 0`)**: If the player clicks a Soul Core and targets another entity with normal cores, `🔄【コア交換】` actions are prioritized above ordinary moves. This guarantees that moving the soul core off a 1-core spirit safely swaps for a normal core instead of destroying the spirit.

## 2. Dynamic Group Visibility

- If `coreGroups.length <= 5`, groups are rendered top-level in the sidebar for immediate discovery.
- Accordion grouping (`.core-all`) is only engaged when there are massive options (e.g. bulk rearrangement $\ge 6$).

## 3. Hand Card Summon Accordion & Option Hierarchy

### 2-Tier Grouping Pattern
1. **Tier 1 (Header/Accordion Grouping)**:
   - Grouped strictly by required cost and total placed core count.
   - **Vanish Label Separation**: Vanish annotations (`⚠️...消滅` / `▲...消滅`) must be stripped from the `costPlacement` key, so choices are not fragmented into dozens of lines simply due to different vanishing donors.
   - **Soul Core Normalization (`normalizeCostPlacement`)**: Soul cores (🔶) and Normal cores (🔷) in cost/placement are normalized to their total count (e.g. `コスト🔷, 配置コア🔷`).
   - Result: Hand cards always collapse into clean rows grouped by placement count (e.g. `1個`, `2個`, `3個`).
2. **Tier 2 (Individual Choice Buttons)**:
   - Specific donor combinations, vanishing warnings (`⚠️#3 ストラレイ消滅`), and exact Soul Core usage (`(支払:R:🔷, 配置:#1:🔶)`) are clearly presented in the second-tier buttons inside the opened accordion.
   - **Placement Soul Core Separation**: Always separate `配置コア 1個 [🔷] (通常コア)` and `配置コア 1個 [🔴] (ソウルコア)` into distinct sub-headers.

## 4. Pending Summon & Core Allocation Invariants (仮召喚・コア要求枠の充足仕様)

- **Donor Net Contribution & Strict Vanishing Guard (`syncPendingSummonWithAllocatedPayment`)**:
  - The server lists `moves` in internal order (`placeFrom` then `payFrom`). Never rely on positional array matching.
  - Calculate the **net contribution per donor** (`{ [donorId]: totalCoresContributed }`) across both payment and placement allocations.
  - **Strict Vanishing Guard**: Even if an action matches contribution counts, if `option.vanishing_card_ids` contains any spirit that currently has remaining cores $> 0$ on the board (i.e. the player did not intentionally empty all its cores), that option is **100% strictly excluded**.

- **Summon Button Enablement on Full Fulfillment (すべて充足したら召喚ボタンを有効化原則)**:
  - `isPendingBothComplete()` evaluates:
    ```javascript
    const isPayComplete = (pendingSummon.allocatedPayDonors?.length === pendingSummon.requiredPayCost);
    const isPlaceComplete = (allocatedPlace.length >= reqPlace);
    return isPayComplete && isPlaceComplete;
    ```
  - Regardless of whether the wizard HUD is in Step 1 or Step 2, once all required payment cores and placement cores are allocated, `#btn-pending-confirm` is immediately enabled (`✅ 召喚確定 (Space)` / `✅ 配置確定 (Space)`).
  - Pressing Space or clicking the button instantly confirms the summon without forcing the player through redundant intermediate steps.
  - If unfulfilled, the button is disabled and displays the explicit shortage count (`⚠️ コスト不足 (あと N 個)` / `⚠️ 維持コア未指定 (あと N 個)`).

- **Dynamic Attention Blinking & Clutter Suppression (点滅枠ハイライトと冗長ラベル抑制)**:
  - Both `.trash-pending-pay-enclosure` (Trash payment tray) and `.card-pending-place-enclosure` (Ghost card maintenance/placement slot):
    - **Unfulfilled State**: Highlighted by an orange dashed border that pulses smoothly (`animation: blink-orange-pay-enclosure 0.9s ease-in-out infinite`).
    - **Fulfilled State**: Blinking stops immediately (`animation: none`), and transitions to a calm green completed border (`border: 1.5px solid rgba(34, 197, 94, 0.75)`).
    - **Clutter Suppression**: Never overlay intrusive text labels (e.g. `コスト支払い中`, `維持コア指定中`) via `::before` pseudo-elements onto cards or trays. Visual dashed/solid border cues and HUD progress indicators provide sufficient, clean feedback without obscuring card art.

## 5. Status Badge Orientation on Card Rotation (疲労・重疲労時のLv/BPバッジ水平維持原則)

- When a card rotates 90° (`.mat-card.exhausted`) or 180° (`.mat-card.heavy-exhausted`), status badges (`.card-badge-lv`, `.card-badge-bp`) counter-rotate (`transform: rotate(-90deg)` / `rotate(-180deg)`) to remain horizontally upright (0°).
- Badges must be repositioned to stay strictly within card corner boundaries without protruding beyond the rotated card perimeter.

## 6. Vanish Detection Preview (`getVanishingCardIds`)

- Always trust the server's `option.vanishing_card_ids` array when present, even if empty (`[]`). Do not bypass with `length > 0` which falsely triggers client fallbacks.
- In client fallback calculations, always inspect `card.category === "NEXUS"` or `card.lv_costs[0] === 0` to guarantee Nexuses are never marked as vanishing (`keep = 0`).
- Ensure `WebCardDto` includes `category` and `lv_costs`.

## 7. Instance ID Binding & Anti-Patterns

- **Attack / Block Action Mapping**:
  - `attackActionsMap` and `blockActionsMap` must be keyed exclusively by `g.target_instance_id` and looked up via `o.id`.
  - Do **NOT** strip prefixes from `g.category` to register or look up by `rawName` / `o.name`. Sequence prefixes (`#1`, `#5`) or duplicate copies of the same card will break name-based lookups.
- **Attacker Identification**:
  - Use `o.is_attacking === true` or `o.id === data.attacking_card_id`. Never parse battle outcome strings to guess the attacker by name.

## 8. Hot-Reloading Static Web Assets & Multi-Playmat Routing

- The server's `loadHtmlFile()` must read directly from the filesystem on every request without keeping an in-memory cache (`cachedHtml`).
- Static file edits to `server/web/playmat2.html` and `server/web/index.html` take effect on F5 without daemon restart.
- E2E Puppeteer scripts connect to SSE streams; always use `waitUntil: 'domcontentloaded'` instead of `'networkidle0'`.

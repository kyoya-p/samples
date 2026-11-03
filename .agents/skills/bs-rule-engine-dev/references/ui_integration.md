# Web Playmat UI Integration Reference

## 1. Direct Mouse Core Movement

### Event Flow
1. User clicks on source core:
   - Reserve click: `handleSourceCoreClick("Reserve", "soul" | "normal")`
   - Field card core click: `handleSourceCoreClick(card.id, "soul" | "normal")`
   - Sets `selectedCoreMoveSource = { sourceId, coreType, label }` and highlights source element.
2. User clicks on target entity:
   - Target card click: invokes `executeDirectCoreMove(fromId, target.id, coreType)`
   - Target reserve click: invokes `executeDirectCoreMove(fromId, "Reserve", coreType)`
3. Action lookup:
   - Traverses `current.groups.flatMap(g => g.options)`
   - Matches candidate where `fromEff.source_id === fromId` and `toEff.source_id === toId`
   - Respects requested `coreType` (prefers soul-core moves if clicked soul core)
   - Dispatches `/api/step?sid=...&act=<index>` via `doAct(chosen.index)`

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
   - Single-option tiers (`isTerminalSg`) remain immediately executable (⏎) without intermediate nesting.

## 4. Vanish Detection Preview (`getVanishingCardIds`)

- Always trust the server's `option.vanishing_card_ids` array when present, even if empty (`[]`). Do not bypass with `length > 0` which falsely triggers client fallbacks.
- In client fallback calculations, always inspect `card.category === "NEXUS"` or `card.lv_costs[0] === 0` to guarantee Nexuses are never marked as vanishing (`keep = 0`).
- Ensure `WebCardDto` includes `category` and `lv_costs`.

## 5. Hot-Reloading Static Web Assets

- The server's `loadHtmlFile()` must read directly from the filesystem on every request without keeping an in-memory cache (`cachedHtml`).
- This ensures any modifications to `server/web/index.html` take effect immediately upon browser reload (F5) without requiring a server daemon restart.

## 6. Instance ID Binding & Anti-Patterns

- **Attack / Block Action Mapping**:
  - `attackActionsMap` and `blockActionsMap` must be keyed exclusively by `g.target_instance_id` and looked up via `o.id`.
  - Do **NOT** strip prefixes from `g.category` to register or look up by `rawName` / `o.name`. Sequence prefixes (`#1`, `#5`) or duplicate copies of the same card will break name-based lookups.
- **Attacker Identification**:
  - Use `o.is_attacking === true` or `o.id === data.attacking_card_id`. Never parse battle outcome strings to guess the attacker by name.
- **Puppeteer / Browser Verification**:
  - The Playmat connects to `/api/events` via SSE (Server-Sent Events), keeping a persistent connection open. Always use `waitUntil: 'domcontentloaded'` instead of `'networkidle0'` in Puppeteer scripts.
  - Action execution uses `POST /api/act` with `{ session, index, viewer }`.
  - When starting `server.exe`, always set the working directory to `2026/BSRuleEngineServer` so `server/web/index.html` is resolved properly.

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

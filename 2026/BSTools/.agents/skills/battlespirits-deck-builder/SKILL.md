---
name: battlespirits-deck-builder
description: Universal agent workflow for Battle Spirits deck construction, Neo4j rule and synergy verification, meta trend analysis, and win-rate maximization.
---

# Universal Agent Skill: Battle Spirits Deck Builder & Meta Optimizer

This skill provides a standardized workflow for AI Agents to construct competitive Battle Spirits decks, verify card rules against the official DB, and analyze winning trends.

---

## 1. Deck Building & Optimization Workflow

```
[1. Meta Analysis]          Analyze current Tier 1/2 archetypes using X/deck_share_trend.html
                                  │
                                  ▼
[2. Archetype Definition]   Choose key cards (e.g. Contract Spirit / Nexus) & synergy family (e.g. 碧雷, 極契約)
                                  │
                                  ▼
[3. DB Query (Neo4j)]       Query local Neo4j DB for card synergy, search targets, and costs
                                  │
                                  ▼
[4. Exact Rule Verification]Fetch verbatim card texts; verify keyword exact matches & extinction/destruction rules
                                  │
                                  ▼
[5. Deck List Output]       Format 40+ card list adhering to 3-copy limits
```

---

## 2. Strict Verification Protocol

1. **推測の完全禁止**: カード番号・コスト・効果・軽減・系統は記憶から回答せず、Neo4j DB または公式サイト検索で完全一致を確認。
2. **相互参照の完全一致**: サーチ効果（例: 「カード名に『○○』を含む」）や煌臨条件は、対象カードのテキストを個別に取得して確認。
3. **ルール裁定遵守**:
   - コア除去等による「消滅」時は「破壊時効果」が発揮されない。
   - 「防げない」効果と「効果を受けない」耐性の優先度（耐性側が優先される公式裁定等）を遵守。

---

## 3. Deck List Standard Format

```markdown
# [デッキ名 / デッキタイプ]
- 契約カード: [枚数] [カード名]
- スピリット: [枚数] [カード名]
- アルティメット: [枚数] [カード名]
- ブレイヴ: [枚数] [カード名]
- ネクサス: [枚数] [カード名]
- マジック: [枚数] [カード名]
- **合計枚数**: [合計] 枚

## 採用理由と主要コンボ
- [キーカードの採用理由とシナジー]
```

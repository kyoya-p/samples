# Battle Spirits Project: Universal Agent Guidelines

This repository contains tools, data pipelines, simulation engines, and analysis applications for the Battle Spirits (バトスピ) TCG.

---

## 1. Directory & Subsystem Map

- **`X/`**: X (Twitter) automated scraping pipeline, LLM tournament deck extraction, and `deck_share_trend.html` interactive meta trend visualizer.
  *(See detailed guidelines in [X/AGENTS.md](file:///C:/Users/kyoya/home26/works/samples/2026/BS/X/AGENTS.md)).*
- **`BSRust/`**: High-performance game engine / battle simulation implemented in Rust.
- **`WebGameBoard/`**: Web-based graphical board and battle simulation interface.
- **`tools/`**: Card data scrapers, Neo4j DB loaders, and image asset utilities.
- **`data/`**: Official card database, Neo4j dump, and parsed datasets.
- **`.agents/skills/`**: Standard project skills for autonomous agent execution.

---

## 2. Core Execution Principles & Verification Rules

1. **Strict Verification (No Hallucinations)**:
   - Never guess card IDs, costs, effects, or archetypes from memory or serial numbers.
   - Always query the local Neo4j DB or perform web search verification.
   - Cross-referencing conditions (e.g. 煌臨 conditions, keyword matches such as "契約") require fetching exact card text for both cards.
2. **Environment & Task Runner**:
   - Use `mise run <task>` (or `./amper.bat`) with explicit arguments. Never run bare `mise run` without arguments.
   - Temporary files must be placed in `./temp/`.
3. **Communication Style**:
   - Concise Japanese, ending in nouns/noun phrases (体言止め).
   - Always attach clickable source links (`file://...` or `https://...`) to substantiate outputs.

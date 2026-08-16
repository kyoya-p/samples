---
name: x-search-scraper
description: Universal agent workflow for scraping X (formerly Twitter) search results with chunked date partitioning, structured tournament deck extraction via LLM, and trend share analysis via interactive HTML visualization.
---

# Universal Agent Skill: X Search Scraper & Meta Analysis Tool (`X/`)

This skill defines the standardized protocol and operational procedures for any AI Agent to execute automated data collection from X (formerly Twitter), extract structured deck/tournament information using an LLM, and perform meta trend share analysis.

---

## 1. Agent Role & Capabilities

When assigned tasks related to X search scraping, tournament report gathering, or TCG meta analysis within this project, the Agent must execute the following standardized 3-step pipeline:

```
[Step 1: X Scraping]        mise run search -- "<Query>" [Date/Chunk Options]
                                  │
                                  ▼ Output: output/x-search-*.json
[Step 2: LLM Extraction]    mise run extract-sb [-- <input.json>]
                                  │
                                  ▼ Output: output/shop-battle-*.csv
[Step 3: Visualization]     output/deck_share_trend.html (Drag & Drop CSV)
```

---

## 2. Standardized CLI Protocol (`X/`)

All operations must be executed using the project task runner (`mise run` or underlying `./amper.bat run -m X -- <cmd>`) from the `X/` directory.

### 2.1 Authentication Pre-check
Before running any automated scraping, verify session existence:
- **Session File**: `X/.auth/x-state.json`
- **Initial Setup / Refresh Command**:
  ```shell
  mise run login
  ```
  *(Launches a headed browser for human login, then automatically persists cookies/session).*

### 2.2 Search & Chunked Scraping (`mise run search`)
Execute targeted query searches across specific date ranges with automatic chunking to avoid X scroll/pagination limits.

```shell
# Standard 6-month search (chunked in 30-day windows, latest tab)
mise run search -- -d 6 -s 30 -l "(バトスピ OR バトルスピリッツ) (優勝 OR 全勝)"

# Explicit date boundaries (YYYY-MM-DD or YYMMDD)
mise run search -- --since 2024-01-01 --until 2024-06-30 -s 30 -l "バトスピ 優勝"
```

#### CLI Parameter Specification
| Parameter | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `query` (Positional) | String(s) | Required | Search query keywords. Use quotes for logical OR/AND groups. |
| `-d, --date` | String | null | Date specifier (e.g. `6` = 6 months, `2401-2406`, `240101-240630`). |
| `--since` | String | null | Start date bound (e.g. `2024-01-01`, `240101`, `2401`). |
| `--until` | String | null | End date bound (e.g. `2024-06-30`, `240630`, `2406`). |
| `-s, --split-days` | Integer | `30` | Number of days per chunk. Automatic partition prevents missing older tweets. `0` disables chunking. |
| `-l, --latest` | Flag | `false` | Search "Latest" tab (`&f=live`) instead of "Top". |
| `-m, --max` | Integer | `MAX` | Global max tweets to collect across all chunks. |
| `--headed` | Flag | `false` | Run browser in headed mode (for debugging/visual confirmation). |
| `-o, --output` | String | Auto | Output JSON filepath. |

#### Output Stream Contract
- **`stderr`**: Real-time progress and completed chunk counts in the format:
  `YYYY-MM-DD 〜 YYYY-MM-DD: <Count>件`
- **`stdout` / File**: Clean JSON output path `output/x-search-<query>-<timestamp>.json`.

---

### 2.3 LLM Tournament Deck Extraction (`mise run extract-sb`)
Processes scraped tweet JSON into normalized, tabular tournament victory records.

```shell
# Auto-detect latest x-search-*.json in output/
mise run extract-sb

# Explicit input and output paths
mise run extract-sb -- output/x-search-example.json -o output/shop-battle-example.csv
```

#### Extraction Schema (CSV Output)
| Field Name | Type | Example | Description |
| :--- | :--- | :--- | :--- |
| `posted_at` | ISO-8601 String | `2026-08-15T14:30:00.000Z` | Tweet creation timestamp. |
| `deck_type` | String | `甲魚`, `アモン`, `赤神星` | Normalized archetype / deck name. |
| `format` | String | `スタンダード`, `エターナル` | Match regulation format. |
| `event_category` | String | `店舗バトル`, `店舗予選`, `公式大会` | Event type category. |
| `raw_text` | String | `...` | Full tweet content (RFC 4180 escaped). |
| `tweet_url` | URL String | `https://x.com/...` | Source tweet permalink. |

---

## 3. Meta Trend Share Analyzer (`X/output/deck_share_trend.html`)

A standalone, zero-dependency HTML/Chart.js web application for dynamic share calculation and trend plotting.

### Interaction Workflow
1. Open [deck_share_trend.html](file:///C:/Users/kyoya/home26/works/samples/2026/BS/X/output/deck_share_trend.html) in any modern browser.
2. Drag and drop any `shop-battle-*.csv` into the central drop zone.
3. The UI dynamically aggregates and displays:
   - **Time Aggregation Interval**: `1週間`, `2週間`, `1か月` (Default), `2か月`, `3か月 (四半期)`, `4か月`, `6か月 (半期)`, `全期間`.
   - **Date Range Filter**: Start date (`いつから`) and End date (`いつまで`) with quick presets (All, 6 Months, 1 Year).
   - **Multi-select Category Filter**: Checkbox chips for `店舗バトル`, `店舗予選`, `公式大会`, etc.
   - **Regulation Tabs**: `全フォーマット`, `スタンダード`, `エターナル`.
   - **Top 8 Archetype Line Chart & Data Table**: Auto-calculated monthly/interval share percentages.

---

## 4. Anti-Blocking & Robustness Protocols

When executing bulk scraping tasks, the Agent must adhere to these operational safety rules:

1. **Chunk Granularity**: Default to `-s 30` (or `-s 14`). Do NOT use excessively small chunks (e.g. `-s 7` for long periods) as it consumes rate-limit quotas rapidly.
2. **Adaptive Cooldowns (Built-in)**:
   - Inter-chunk delay: `5.0s - 8.0s` randomized.
   - Zero-tweet / block backoff: +10.0s cooldown.
   - 2-Stage Retry: 10s wait reload (Stage 1), 60s cooldown reload (Stage 2).
3. **Session Reuse**: Always ensure `.auth/x-state.json` is preserved and not re-created unless expired.

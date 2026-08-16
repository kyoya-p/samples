# CLI Reference: X Tool (`X/`)

Comprehensive CLI argument and option specification for AI Agents.

---

## 1. `mise run search` (or `..\amper.bat run -m X -- search`)

Searches X via Playwright using saved authentication session.

### Syntax
```shell
mise run search -- [OPTIONS] <QUERY...>
```

### Options
| Option | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `query` | Arguments | Required | Search query string. Supports boolean operators `(A OR B) (C OR D)`. |
| `-d, --date` | Option | `null` | Date filter token: `6` (last 6 mo), `2401-2406` (Jan-Jun 2024), `240101-240630`. |
| `--since` | Option | `null` | Explicit start date (`YYYY-MM-DD`, `YYMMDD`, `YYMM`). |
| `--until` | Option | `null` | Explicit end date (`YYYY-MM-DD`, `YYMMDD`, `YYMM`). |
| `-s, --split-days` | Option | `30` | Number of days per chunk (default 30). Set `0` to disable chunking. |
| `-l, --latest` | Flag | `false` | Scrapes from the "Latest" tab (`&f=live`) instead of "Top". |
| `-m, --max` | Option | `MAX` | Max total tweets to collect across all chunks. |
| `-o, --output` | Option | Auto | Target output JSON filepath. |
| `--headed` | Flag | `false` | Launches browser with visual UI (for debugging). |

---

## 2. `mise run extract-sb` (or `..\amper.bat run -m X -- extract-sb`)

Batches scraped JSON tweets into Gemini API for structured extraction.

### Syntax
```shell
mise run extract-sb -- [OPTIONS] [INPUT_JSON]
```

### Options
| Option | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `input` | Argument | Latest in `output/` | Path to `x-search-*.json`. |
| `-o, --output` | Option | Auto | Output CSV path (`output/shop-battle-<timestamp>.csv`). |
| `-m, --model` | Option | `gemini-2.5-flash-lite` | Gemini model name. |
| `-b, --batch-size` | Option | `25` | Number of tweets processed per LLM call. |

---

## 3. `mise run login`

Launches a headed Chromium browser for human authentication. Saves session state to `X/.auth/x-state.json`.

---

## 4. `mise run setup`

Downloads and updates Amper wrapper and installs Playwright Chromium binaries.

# Workflow Examples & Recipes: X Tool (`X/`)

Practical operational recipes for AI Agents executing common TCG / X scraping tasks.

---

## Recipe 1: 6-Month Meta Analysis Run
Scrapes the last 6 months of tournament victory tweets, extracts data, and prepares it for visualization.

```shell
# 1. Scrape with 30-day chunks (Latest tab)
mise run search -- -d 6 -s 30 -l "(バトスピ OR バトルスピリッツ) (優勝 OR 全勝)"

# 2. Extract structured victory records (CSV)
mise run extract-sb

# 3. View in Browser
# Open X/output/deck_share_trend.html and drop the generated CSV.
```

---

## Recipe 2: Targeted Historical Range Scraping
Scrapes a specific historical period (e.g. Q1 2024).

```shell
mise run search -- --since 2024-01-01 --until 2024-03-31 -s 30 -l "バトスピ 優勝"
mise run extract-sb
```

---

## Recipe 3: Quick Smoke Test (Debugging)
Runs a fast 1-month test with max 10 tweets in headed mode.

```shell
mise run search -- -d 1 -m 10 --headed "バトスピ 優勝"
```

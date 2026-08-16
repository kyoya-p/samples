# Anti-Blocking & Rate Limiting Runbook: X Scraping

This guide explains X's anti-scraping mechanisms and the evasion protocols implemented in `Search.kt`.

---

## 1. Rate Limiting Mechanics on X

X enforces strict rate limits per IP and authentication session:
1. **15-Minute Rolling Window**: Fast page transitions or excessive requests within a 15-minute window trigger an HTTP 429 / empty timeline response.
2. **Behavioral Bot Detection**: Constant-interval scrolling (e.g. exactly 1.0s) or instantaneous URL hopping triggers temporary IP throttling.
3. **Scroll Limits**: A single query URL caps out after several hundred tweets. Dividing the date range into 30-day chunks (`-s 30`) bypasses this limit.

---

## 2. Evasion Strategy (Implemented in `Search.kt`)

| Mechanism | Implementation | Rationale |
| :--- | :--- | :--- |
| **Adaptive Chunk Cooldown** | 5.0s - 8.0s random pause between date chunks | Avoids rapid-fire page loads that flag bot behavior. |
| **Zero-Tweet Backoff** | +10.0s extra delay when a chunk returns 0 tweets | Protects session from escalating into a full ban. |
| **Scroll Jitter** | 1.8s base + 0.5s-1.5s random jitter | Mimics human scrolling cadence. |
| **2-Stage Retry Backoff** | Stage 1 (10s retry) → Stage 2 (60s cooldown retry) | Allows transient rate limits to expire and recover automatically. |

---

## 3. Best Practices for Agents

1. **Recommended Chunk Size**: Always use `-s 30` (or `-s 14`). Never use `-s 7` for 6+ month spans.
2. **Session Preservation**: Do not delete `.auth/x-state.json` unless login has truly expired.
3. **Execution Patience**: The scraper deliberately waits during cooldowns. Do not terminate tasks prematurely.

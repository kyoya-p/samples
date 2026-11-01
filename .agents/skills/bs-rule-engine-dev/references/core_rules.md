# Battle Spirits Rule Reference & Engine Implementation

## 1. Official Comprehensive Rules Compliance

### Maintain Cost (維持コスト)
- **Spirits**: Lv1 cost is typically $\ge 1$. Placing 0 cores on a Spirit triggers immediate vanish (消滅).
- **Nexus**: Lv1 cost is $0$. A Nexus remains in the arena with 0 cores without vanishing.
- Engine implementation:
  ```kotlin
  val lv1Cost = fc.lvCosts.firstOrNull() ?: 0
  val vanish = afterTotal < lv1Cost
  ```

### Core Movement Combinatorics
- For any donor with $n$ normal cores and $s$ soul cores:
  - If $n > 0$, moves for $1 \dots n$ normal cores are created.
  - If $s > 0$, single soul core move (0 normal, 1 soul) is created.
  - If $s > 0$ and $n > 0$, mixed moves for $k$ normal + 1 soul ($k \in 1 \dots n$) are created.

### Loop Prevention via CoreWindow
- Instead of tracking symmetric pairs (`A <-> B`), the engine tracks directed transitions: `"${from}->${to}"`.
- Sequential forward moves (e.g. moving 1 core from R to card A, then another core from R to card A) are permitted.
- Reversing the move (card A back to R) in the same core window is blocked.

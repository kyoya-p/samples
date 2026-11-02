# Handoff Report — Bend並列アーキテクチャ・MCTS設計・状態表現調査

## 1. 観測 (Observation)

### 1.1 プロジェクト要件およびタスク指令
- **タスク指令** (`C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\teamwork_preview_explorer_survey_3\DISPATCH.md` 15-25行目 [file:///C:/Users/kyoya/home26/works/samples/2026/.agents/teamwork/teamwork_preview_explorer_survey_3/DISPATCH.md]):
  > 1. Analyze Bend language capabilities and idiom:
  >    - Data structures in Bend: ADTs / objects / records, tuples, trees, lists, Scott encoding or pattern matching.
  >    - Concurrency / parallelism primitives: `bend` statements, `fork`, parallel tree recursion, divide-and-conquer reducers.
  >    - Memory and performance characteristics in HVM (Interaction Combinators): avoiding bottlenecks, lazy evaluation vs strict evaluation.
  > 2. Formulate state representation strategy for Battle Spirits in Bend:
  >    - Compact immutable representation of game state.
  >    - Deterministic fast-forward simulation (rollout function: `state -> winner/score`).
  >    - Move generation / branching: parallel tree expansion (e.g. `bend state = initial { ... }` or branching over legal actions).
  >    - Monte Carlo Tree Search (MCTS) support: parallel rollouts, UCT evaluation, throughput benchmarking (moves/sec).
  > 3. Recommend file layout and compilation/execution patterns for high throughput.
- **元リクエスト受入基準** (`C:\Users\kyoya\home26\works\samples\2026\.agents\teamwork\ORIGINAL_REQUEST.md` 28-31行目 [file:///C:/Users/kyoya/home26/works/samples/2026/.agents/teamwork/ORIGINAL_REQUEST.md]):
  > Parallel Execution & Performance:
  > - Engine compiles and executes under Bend (CPU/HVM or GPU targets).
  > - Parallel rollout/simulation benchmark executes multiple concurrent game paths and outputs moves/sec or throughput metrics without deadlocks or race conditions.

### 1.2 Bend言語およびHVM2の仕様・特性
- **公式リポジトリ・パッケージ**:
  - `bend-lang` v0.2.38 / `hvm` v2.0.22 (crates.io [https://crates.io/crates/bend-lang/0.2.38], [https://crates.io/crates/hvm/2.0.22])
  - プラットフォーム: POSIX 必須。Windows 上では WSL2 (Ubuntu 24.04, gcc 13.3.0) 経由で動作可能 (Explorer 1 観測結果 [file:///C:/Users/kyoya/home26/works/samples/2026/.agents/teamwork/teamwork_preview_explorer_survey_1/handoff.md])。
  - バックエンド: `bend run` (Rustインタプリタ), `bend run-c` (C並列ランタイム/マルチコアCPU), `bend run-cu` (CUDA/NVIDIA GPU)。本環境は Hyper-V 仮想マシンのため、C並列ランタイム (`bend run-c`) が最大スループット標的。
- **データ型・数値表現**:
  - HVM2 の基本数値型は 24bit (`u24`, `i24`, `f24`)。Interaction Combinators の 64bit ノード構造（タグ 3bit + 各ポート 29bit）に unboxed 格納されるため、`u24` 演算はアロケーション不要で最高速。
  - 型定義 (`type`, `object`): `object Point { x, y }` または `type Tree: Node { ~lft, ~rgt } Leaf { val }`。
  - 再帰指定子 `~`: `fold` 構文で自動再帰畳み込みを行うフィールドに指定。
- **並列プリミティブ**:
  - `bend var = seed: when cond: fork(new_seed) else: base` により木構造（Anamorphism / 分割統治）を並列展開。
  - `fold expr: case Type/Cons: ...` により木構造（Catamorphism）を並列集約。
- **HVMのメモリ・性能ボトルネック**:
  - **線形性 (Affine/Linear usage)**: 変数を1回のみ使用する場合、ポインタ配線移動のみ ($O(1)$)。
  - **複製ノード (Dup Node Bloat)**: 変数を複数箇所で参照（例: `fork` の両分岐へ同一状態を渡す）すると `dup` ノードが生成され、深いネスト構造の複製には $O(N)$ のグラフ書き換えコストが発生。
  - **平坦・非ポインタ指向**: ネストした連結リスト (`List/Cons`) の複製・走査は直列化とメモリ肥大化を招くため、状態は固定長タプル・固定長レコード・24bit整数ビットマスクで表現することが至上命題。

### 1.3 バトルスピリッツの既存リファレンス資産
- **既存 Kotlin 実装** (`C:\Users\kyoya\home26\works\samples\2026\BSTools\model\src\Rules.kt` [file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/Rules.kt], `Model.kt` [file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/Model.kt]):
  - ターン進行: `START` -> `CORE` -> `DRAW` -> `REFRESH` -> `MAIN` -> `ATTACK_START` -> `ATTACK_DECLARATION` -> `FLASH_TIMING` -> `BLOCK_DECLARATION` -> `BATTLE_RESOLUTION` -> `MAIN_2` -> `END` (`Model.kt` 69-84行目)。
  - コア領域: Life (初期5), Reserve (初期4: 通常3+Soul1 または通常4), Field (各スピリット), Trash, Void (`Model.kt` 89-95行目)。
  - 先攻1ターン目制約: コアステップ・アタックステップ・メイン2ステップ禁止 (`Rules.kt` 76-80行目)。
  - スピリット消滅: コア数がLv1維持コア数未満（0個）で即座にトラッシュ送り、乗っていたコアはリザーブへ回収 (`Rules.kt` 760-771行目)。
  - 戦闘解決: ブロック宣言時は BP 比較（敗者破壊・同値両者破壊・ライフ保護）、ノーブロック時はシンボル数分ライフ減少（コアはライフからリザーブへ移動）(`Rules.kt` 888-939行目)。

---

## 2. 論理チェーン (Logic Chain)

### 2.1 HVM2メモリモデルに基づく超軽量状態表現の導出
1. **[Observation 1.2 より]** HVM2 は Interaction Combinators 上で動作し、ノードの複製（`dup`）にはグラフ書き換えコストを要する。大規模ロールアウト（$2^{10} \sim 2^{16}$ 並列）において、状態内に深いリストやポインタ連鎖を含めると、`fork` 展開時にメモリ爆発（Graph bloat）が発生する。
2. **[Observation 1.2 より]** 24bit 符号なし整数 (`u24`) は HVM2 のノード内に直接 unboxed で保持され、複製コストがゼロ（アロケーションなし）である。
3. **[Observation 1.3 より]** カードの固定スペック（コスト、軽減シンボル、各Lvの維持コアとBP）は対局中に一切変動しない静的メタデータである。
4. **[推論]** 静的メタデータを `GameState` に保持せず、純粋関数 `card_info(id) -> (cost, red, lv1_c, lv1_bp, lv2_c, lv2_bp, sym)` として外部化することで、`GameState` は動的な最小情報（ID, コア数, 状態フラグ）のみを保持すればよくなる。
5. **[結論]**
   - 盤面上のスピリットスロットを固定数（例: 各陣営最大4体）のタプル `(s0, s1, s2, s3)` で保持。
   - スピリット単体表現: `(card_id: u24, cores: u24, is_exhausted: u24)`
   - プレイヤー状態: `(life: u24, reserve: u24, trash: u24, deck_cnt: u24, hand_bits: u24, spirits)`
   - グローバル状態: `(turn: u24, active_player: u24, step: u24, battle: BattleState, p1: Player, p2: Player)`
   - これにより `GameState` 全体が固定サイズの平坦タプルとなり、1回の状態複製コストが $O(1)$ オーダーの極小ノード書き換えに収まる。

### 2.2 純粋関数型 Splittable PRNG による決定論的並列展開
1. **[Observation 1.2 より]** Bend には標準の可変乱数生成器が存在せず、純粋関数型の決定論的 PRNG が不可欠。
2. **[推論]** 並列ツリー展開において、左分岐と右分岐に同一の乱数系列を渡すと同一のロールアウト結果となり探索効率が崩壊する。
3. **[推論]** 24bit 整数の算術剰余特性を活かした 線形合同法 (LCG) または Xorshift24 による **Splittable PRNG** パターンを構築する:
   - `rng_next(s) = (s * 1664525 + 1013904223) & 0xFFFFFF`
   - `rng_split(s) = ((s * 2 + 1) & 0xFFFFFF, (s * 2 + 2) & 0xFFFFFF)`
4. **[結論]** `bend` の `fork` 呼び出し時に親シード $s$ から左子シード $s_L$ と右子シード $s_R$ を分岐生成することで、排他制御・同期なしに統計的独立性を持つ並列シミュレーションが保証される。

### 2.3 `bend` / `fork` による並列ロールアウトと集約アーキテクチャ
1. **[Observation 1.1, 1.2 より]** `bend` は Anamorphism（ツリー展開）、`fold` は Catamorphism（ツリー集約）を担当する。
2. **[推論]** 目標並列度 $K = 2^D$（例: $D=10 \Rightarrow 1024$ 並列、$D=14 \Rightarrow 16384$ 並列）のロールアウトを実行する場合、深さ $D$ の2分木を `bend` で展開する:
   ```bend
   def parallel_rollouts(state, max_depth, seed):
     bend d = 0, s = seed:
       when d < max_depth:
         s_l = rng_left(s)
         s_r = rng_right(s)
         wins = fork(d + 1, s_l) + fork(d + 1, s_r)
       else:
         wins = rollout(state, s, 30) # 最大30ステップの高速順シミュレーション
     return wins
   ```
3. **[推論]** HVM2 の C並列ランタイム (`bend run-c`) は、この二分木ノードをワーカースレッドプール（CPUコア数に応じた並列度）に自動分散して評価し、末尾の `wins = left + right` でロックフリー・加算リダクションを行う。
4. **[結論]** 共有メモリ競合・デッドロック・レースコンディションが原理的に発生しない純粋関数型超並列シミュレータが成立する。

### 2.4 MCTS (モンテカルロ木探索) アーキテクチャの選定
1. **[推論]** 関数型言語における MCTS 実装には以下の2方式が存在する:
   - **方式A: Root-Parallel Batched MC (ルート並列・バッチロールアウト)**:
     着手可能手 $A = [a_1, a_2, \dots, a_m]$ の各手に対して、`parallel_rollouts(apply_action(s, a_i), depth, seed_i)` を並列起動し、勝率最高手を選択。
   - **方式B: Persistent Functional MCTS Tree (永続木探索・UCB1探索)**:
     木構造 `type MctsNode: Leaf { state, visits, wins } Node { ... }` を定義し、選択（Selection）・展開（Expansion）・バッチシミュレーション（Simulation）・逆伝播（Backpropagation）を木書き換えとして実行。
2. **[推論]** 高スループット（moves/sec最大化）およびバトスピのような不完全情報・分岐の多いゲームにおいては、**「方式A（ルート並列バッチ）」** をベースコアとし、その上位に **「方式B（深さ $k$ のUCB1段階的展開）」** を結合するハイブリッドアーキテクチャが最も HVM2 の特性に合致する。
3. **[結論]** エンジン層として `rollout_fast(state, seed, max_steps)` を提供し、MCTS層として `mcts_root_parallel(state, moves, rollouts_per_move, seed)` および `mcts_step(node, seed)` の双方を提供する。

### 2.5 ベンチマーク手法 (moves/sec) の定式化
1. **[Observation 1.2 より]** Bend 実行統計には `Rewrites` (グラフ書換総数), `Time` (秒), `MIPS` (百万書換/秒) が出力される。
2. **[推論]** ドメイン固有の `moves/sec`（毎秒評価着手数）を計測するため、各ロールアウト関数は `(winner: u24, steps_taken: u24)` のタプルを返す。
3. **[推論]** $K$ 回の並列ロールアウトで消費された合計ステップ数 $\text{TotalMoves} = \sum_{i=1}^K \text{steps}_i$ を並列加算集約し、プログラム出力として `(total_moves, total_wins)` を返却する。
4. **[結論]** 実行ラッパー（PowerShell / Bash スクリプト）が `bend run-c` の経過時間 $T$ と戻り値 $\text{TotalMoves}$ から $\text{Moves/sec} = \frac{\text{TotalMoves}}{T}$ を算出し、HVM2ネイティブの MIPS と併せてスループット指標を提示する。

---

## 3. 注意事項・保留点 (Caveats)

1. **WSL2 依存と実行環境**:
   - Bend および HVM2 は Windows ネイティブ非対応のため、実行・テスト・ベンチマークは WSL2 (Ubuntu 24.04) 上で実施する必要がある。
2. **GPU (CUDA) バックエンドの除外**:
   - 実行ホストが Hyper-V 仮想マシンのため、物理 NVIDIA GPU が存在しない (`nvidia-smi` 未検出)。本設計は CPU マルチコア並列 (`bend run-c`) を最優先ターゲットとする。
3. **24bit 整数のオーバーフロー**:
   - HVM2 の数値型 `u24` は $0 \sim 16,777,215$ (約1600万) の範囲。ロールアウト総数やステップ数累計が 1600万 を超える超大規模ベンチマークを行う場合は、上位ビットと下位ビットを分けた2ワード表現 `(hi, lo)` が必要。実用的な探索（1回あたり数千〜数万手）では 24bit で十分収まる。
4. **カードプールのスコープ**:
   - 初期検証では、基本色（赤・白・緑等）の代表的スピリット、ネクサス、マジック数枚（合計8〜16種類のカードID）によるミニマルデッキでのシミュレーションに絞る。複雑な常時置換効果や創界神等の特殊カードは初期コアエンジンから除外する。

---

## 4. 結論 (Conclusion)

### 4.1 推奨ディレクトリ構成
```
battle_spirits_bend/
├── src/
│   ├── types.bend          # 状態・カード・ステップ・アクション定義
│   ├── cards.bend          # 静的カードデータベース (純粋関数 lookup)
│   ├── prng.bend           # 24bit Splittable PRNG (LCG / Xorshift)
│   ├── rules.bend          # コア保存則、ステップ進行、戦闘解決 (BP/Life)
│   ├── moves.bend          # 合法手生成器 (Main, Attack, Block, Flash)
│   ├── rollout.bend        # 高速順ロールアウト関数 (state, seed, max_steps)
│   └── mcts.bend           # bend/fork による並列シミュレーション & UCT
├── tests/
│   ├── test_invariants.bend # コア保存則・不変条件検証テスト
│   ├── test_rules.bend      # ステップ遷移・召喚・戦闘BP解決テスト
│   └── test_mcts.bend       # 並列ロールアウト収束テスト
├── bench/
│   └── benchmark.bend      # 2^D 並列ロールアウト & moves/sec 計測
├── Makefile / mise.toml    # WSL2 実行用ビルドタスク定義
└── README.md
```

### 4.2 コアデータ構造設計 (Bend 形式)
```bend
# スピリットインスタンス: (card_id, cores, is_exhausted)
# card_id: 0 は空スロット
object SpiritSlot { card_id, cores, exhausted }

# プレイヤー状態
object Player {
  life: u24,
  reserve: u24,
  trash: u24,
  deck_count: u24,
  hand_count: u24,
  # 盤面スピリットスロット 4枠 (固定長タプルでDUPコスト極小化)
  s0: SpiritSlot,
  s1: SpiritSlot,
  s2: SpiritSlot,
  s3: SpiritSlot
}

# バトル進行サブ状態
object Battle {
  attacking_slot: u24, # 0=なし, 1..4=スロット番号
  blocking_slot: u24,  # 0=なし, 1..4=スロット番号
  flash_priority: u24, # 1=防御側, 2=攻撃側, 0=なし
  pass_count: u24      # 連続パス回数 (2で終了)
}

# ゲーム全体状態
object GameState {
  turn: u24,
  active_player: u24, # 1 または 2
  step: u24,          # 0=START, 1=CORE, 2=DRAW, 3=REFRESH, 4=MAIN, 5=ATTACK, 6=FLASH, 7=BLOCK, 8=BATTLE, 9=END
  battle: Battle,
  p1: Player,
  p2: Player
}
```

### 4.3 高速静的カード定義 (Lookup Table)
```bend
# card_id -> (cost, reduction_symbols, lv1_cost, lv1_bp, lv2_cost, lv2_bp, symbol_color)
def card_lookup(id):
  match id:
    case 1:
      # 赤スピリット (コスト1, 軽減1, Lv1:1個/1000BP, Lv2:2個/3000BP, 赤シンボル=1)
      return (1, 1, 1, 1000, 2, 3000, 1)
    case 2:
      # 赤スピリット中型 (コスト3, 軽減2, Lv1:1個/3000BP, Lv2:3個/5000BP, 赤シンボル=1)
      return (3, 2, 1, 3000, 3, 5000, 1)
    case 3:
      # 白スピリット防御型 (コスト2, 軽減1, Lv1:1個/2000BP, Lv2:2個/4000BP, 白シンボル=2)
      return (2, 1, 1, 2000, 2, 4000, 2)
    case _:
      return (0, 0, 0, 0, 0, 0, 0)
```

### 4.4 並列ロールアウト & ベンチマークコード設計
```bend
# 単一ゲームの高速ランダム順シミュレーション (末尾再帰)
def rollout(state, seed, steps_left):
  # 終了判定: ライフ0 または デッキ0 または ステップ上限
  open GameState: state
  p1_life = state.p1.life
  p2_life = state.p2.life
  if p1_life == 0:
    return (2, 30 - steps_left) # P2勝利, 消費ステップ
  elif p2_life == 0:
    return (1, 30 - steps_left) # P1勝利, 消費ステップ
  elif steps_left == 0:
    # ライフ優劣で仮判定
    if p1_life > p2_life:
      return (1, 30)
    else:
      return (2, 30)
  else:
    # 決定論的合法手選択 & 状態遷移
    next_seed = (seed * 1664525 + 1013904223) & 0xFFFFFF
    action = choose_random_action(state, next_seed)
    next_state = step_transition(state, action)
    return rollout(next_state, next_seed, steps_left - 1)

# bend/fork による 2^depth 並列ロールアウト
def run_parallel_bench(initial_state, depth, base_seed):
  bend d = 0, s = base_seed:
    when d < depth:
      s_l = (s * 2 + 1) & 0xFFFFFF
      s_r = (s * 2 + 2) & 0xFFFFFF
      (w1_l, m_l) = fork(d + 1, s_l)
      (w1_r, m_r) = fork(d + 1, s_r)
      res = (w1_l + w1_r, m_l + m_r)
    else:
      (winner, moves) = rollout(initial_state, s, 30)
      p1_win = if winner == 1: 1 else: 0
      res = (p1_win, moves)
  return res
```

---

## 5. 検証方法 (Verification Method)

### 5.1 動作検証コマンド (WSL2経由)
1. **Bend構文・型チェック**:
   ```bash
   wsl bash -c "cd /mnt/c/Users/kyoya/home26/works/samples/2026/battle_spirits_bend && bend check src/rules.bend"
   ```
2. **単体ルール・不変条件テスト実行 (インタプリタ)**:
   ```bash
   wsl bash -c "cd /mnt/c/Users/kyoya/home26/works/samples/2026/battle_spirits_bend && bend run tests/test_invariants.bend"
   ```
3. **C並列バックエンドによる並列ロールアウト実行**:
   ```bash
   wsl bash -c "cd /mnt/c/Users/kyoya/home26/works/samples/2026/battle_spirits_bend && bend run-c bench/benchmark.bend -s"
   ```
   - `-s` オプションにより、書換総数 (Rewrites), 実行時間 (Time), MIPS が出力されることを確認。
4. **スループット (moves/sec) 算出**:
   `benchmark.bend` が返却する `(total_p1_wins, total_moves)` 出力値と `Time` より、`total_moves / Time` を計算してスループット基準を満たすことを確認。

### 5.2 失効条件 (Invalidation Conditions)
- Bend/HVM2 の仕様更新により、`u24` 以外の64bit型が標準となりオブジェクトメモリレイアウトが刷新された場合。
- ホスト環境に CUDA 対応 NVIDIA GPU が追加され、`bend run-cu` による GPU 並列が最優先バックエンドとなった場合。

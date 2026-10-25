---
name: bstoolscla-rl
description: >-
  BSToolsCla (BSTools から分離した GRU 価値ネット + TD(λ) 自己対戦強化学習 + Playmat) の設計・運用知見。
  学習/対戦/リーグ/選択肢の列挙/ミニマックス探索/並列化/Playmat/重みファイルを扱う作業で参照する。
---

# BSToolsCla 強化学習 (GRU 価値ネット + TD(λ))

場所: `C:\Users\kyoya\projs\samples\2026\BSToolsCla`。Kotlin/Native (mingwX64)、Amper (`.\kotlin.bat`)、タスクは `mise.toml`。

## 構成

| モジュール | 内容 |
|---|---|
| `engine/` | ルールエンジン (BSTools の model から移植、ヒューリスティック評価は全廃)。列挙: `Rules.kt` (従来/全合法手)、`CompactActions.kt` (圧縮)、`LevelActions.kt` (Lv単位、既定)、`Rearrange.kt` (一括再配置) |
| `rl/` | `GruValueNet.kt` (純Kotlin GRU、9,249パラメータ、BPTT+Adam)、`Agents.kt` (NetAgent: 深さN ミニマックス + 幅k)、`Trainer.kt`、`League.kt`、`Game.kt`、`Parallel.kt` |
| `valuerl/` | CLI: selftest / train / match / league / depthbench / branch / inspect / value |
| `playmat/` | 盤面UI サーバー (8090、タブ別セッション、SSE)。`web/index.html` |

重み: `weights/<派生元>_<派生先>.gru.weight`、最良 `-best`、リーグ加入スナップショット `-p<番号>`。

## 選択肢の列挙 (`--actions`、学習と Playmat で共通)

| 値 | 内容 | 注意 |
|---|---|---|
| `level` (既定) | プレイはリザーブ払い・Lvの境目の配置 (ソウルコア有無)。コアは「カードのLv変更 (境目の数へ)」「ソウルコア移動」を各対象プレイ等の間に1回 (`GameState.coreWindow`) | 各カードのLvとソウルコアの場所の全組み合わせに到達。候補は平均6程度、循環しない |
| `all` | プレイの支払い・配置の全組み合わせ + 一括再配置 (最終配置を1手で選ぶ) | 配置数 N×C(X+N−2, N−1)。場所7・コア12で86,632通り、場所8・コア15で930,240通り。**学習に使うとメモリ13GB超・極端に遅い** |
| `compact` | 支払いは既定ルール1通り、コア移動は「意味のある再配置」 | コア移動を延々と続ける引き延ばしが起きる |
| `full` | 従来の全列挙 (件数上限あり) | |

- コアを何手動かしても結果は最終配置で決まる → 再配置は「同じ対象を続けて動かさない」で選べる結果を減らさずに循環を断てる
- 同一局面に戻る手の除外 (`markRepetitions`) だけでは循環は止まらない (配置の数が手数上限より多い)
- Playmat の選択肢の末尾の括弧は UI がコア配分の照合に使う: 必ず `(支払:R:🔷🔷, 配置:R:🔷|なし)` の形式 (数なしだとネクサス配置等が「一致する手なし」になった)

## 探索 (`--depth N` / `--width k`)

- 深さは「選択の余地がある局面」の数。自分の局面は最大・相手は最小 (αβ)。選択肢1つの局面は深さを数えない
- 末端は「その手を選んだプレイヤーの視点」で評価し、相手視点なら符号反転 (学習データと同じ視点)
- 幅 k (既定1。0=制限なし): 1手先読みの上位 k 手だけ深く読み、その中で最大の手を選ぶ。**幅1では深さを上げても選ぶ手が変わらない**ので、幅1で深さ1以外を指定するとエラー (`searchSettingsError`、train/match/league/Playmat)。深さ2以上は幅2以上か0で。ルートで読まない手は「読んだ手の最小値」未満に切り詰める
- 深さ1つで約10倍の時間。N≥2 の学習では λ-return の次局面の値に対局中の探索値を使う

## 学習 (`mise run train ...`)

- 目標値: λ-return (λ=0.7)。終局 ±1、手数上限の引き分けは −min(0.9, 0.1+0.8×引分率)
- `--step-penalty` (1手ごとの報酬 −c、既定 0.001): **副作用あり** (長い引き分けの目標値が −1 に切り詰められ負けと同じになる)。
  `level` 列挙なら循環が構造上ないので **0 を指定**
- `--max-steps`: `level` 列挙は1ターン約12手。デッキ切れ (約70ターン) より前に400手で切れて引き分けになるので **1500** にする
- 最良の更新: 直前の最良との直接対戦 ≥ `--promote` (全局引き分けなら免除) かつ リーグの Elo が最良を上回る
- ネットが変わったら学習用の局面・目標値は破棄する (ReplayBuffer は反復ごとに clear)

## 並列化 (`--threads`、既定 = 論理プロセッサ数)

- 自己対戦・評価対局・リーグ・match・league の各局を Kotlin/Native の Worker で同時に指す (`rl/src/Parallel.kt`)
- 安全条件: ネットの評価は読むだけ (作業配列は呼び出しごと)、エージェントは局ごとに `threadCopy()`、
  カード/デッキのキャッシュは `Parallel.warmUp` で先に埋める、乱数は局ごとに種を事前決定 (スレッド数で結果が変わらない。selftest で確認)
- `actionSet` は `@ThreadLocal` にしない (Worker から見えなくなる)
- 実測: 12局 1スレッド 4秒 → 6スレッド 1.5秒 (Ryzen 5 5500、論理6)

## 検証の手順

1. `valuerl.exe selftest` (約20秒。勾配検査・列挙の妥当性・一括再配置の件数=公式・循環なし・並列の再現性など)
2. 強さは同じ列挙・同じ深さで比べる: `mise run match --a X --b Y --games 200 --depth 1` (先後交互、序盤4ターン温度0.05)
3. 総当たり: `mise run league --models a,b,c --games 40`

## 落とし穴 (実際に起きたもの)

- `snapshot` がデッキを共有 → 評価のたびに実デッキが減る (コピーで修正済み)
- Playmat の POST 本文が分割到着して無視される (Content-Length まで待つ)
- 圧縮列挙の詳細文字列にコア数がなく、UI のコア配分照合が失敗 (ネクサス配置失敗)
- 表示名 `#1 フラッティ` に UI がさらに番号を付けて `#1 #1` (番号を除いて照合)
- 全合法手 (`all`) で並列対局 → 配置の組み合わせ爆発でメモリ 13GB 超 (`level` を既定に)
- PowerShell の `curl` で JSON を渡すと引用符が崩れる (Node の fetch か Bash の curl を使う)
- POST は `?session=` をパスに付けると 404 (本文の `session` を使う)

## 実績 (2026-10-26)

学習 g2_g3: `mise run train --resume g1_g2-best.gru.weight --games 32 --iters 60 --step-penalty 0 --pool ... -- --max-steps 1500 --updates 300 --eval-games 40 --league-games 20`
(level列挙・深さ1・6スレッド、約1.3分/反復、60反復で約80分)。

対 g1_g2-best の勝率 (各200局、先後交互):

| モデル | 深さ1・幅1 | 深さ2・幅2 |
|---|---|---|
| g2_g3-p1 | 71.8% | 65.2% |
| g2_g3-p2 | 65.5% | 56.8% |
| g2_g3-p3 | 75.5% | 49.0% |

- **「強い」は測る深さ・幅で変わる**。p3 は深さ1でだけ強い。判定は学習と同じ設定と、探索を深くした設定の両方で行う
- 昇格判定 (40局) は誤差が大きい (標準誤差 ±8%)。p1/p2/p3 のうち最終的な総当たり (深さ2・幅2) の Elo は p1 > p3 > p2。最終モデルは p3 より弱かった
- 既定の幅は 1 (0 = 制限なし)。深さ1では幅は効かない
- 深さ2以上で学習 (h1_h2, h1_h2all) は強くならなかった。強化の主因は列挙 (level)・手数上限1500・並列による大量の対局
- スライド: https://claude.ai/artifact/U6w3vtDhTKKqwjpxePfc9n (Slides type、27枚)。更新はこの URL へ publish (`project/` 配下)

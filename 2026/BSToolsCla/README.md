# BSToolsCla

BSTools (バトルスピリッツ シミュレータ) の **RNN局面評価と強化学習部分を作り直した** プロジェクト。
ルールエンジンは BSTools の `model` をコピーして流用し、評価器と学習は新規実装。

## 構成

| モジュール | 内容 |
|---|---|
| `engine/` | ルールエンジン (BSTools `model` から `Model.kt` / `Rules.kt` / `CardLoader.kt` / `GameSetup.kt` / `cards` / `effects` をコピー) + 圧縮選択肢列挙 `CompactActions.kt` |
| `rl/` | 強化学習の本体ライブラリ (特徴量・GRU価値ネットワーク・エージェント・対局・リーグ・学習) |
| `valuerl/` | 学習・対戦・計測の CLI (`rl` を使う) |
| `playmat/` | 盤面 Web UI とサーバー (BSTools の GameServer + Playmats を移植し、評価を GRU に置換) |
| `decks/` | デッキ定義 (BSTools からコピー) |
| `weights/` | 重みファイル |

カードマスタは BSTools と同じく `~/.bscards/yaml/<型番>.yaml` から読む。

### BSTools からの変更点 (engine)

- **手書きヒューリスティック評価をすべて廃止** (`EvalWeights.kt` 削除、召喚・コア移動・アタック・ブロック・ブロックしない・同一局面・ガリザロト指定の評価式を除去)。エンジンは評価値を付けず `GameAction.eval` は常に0.0。評価は価値ネットワークのみ (selftest で検査)
- `enumerateActions` の RNN/KANN 直結を廃止し、差し替え口 `actionEvaluator` に置換 (評価器をエンジンに依存させない)
- `CardScript.enumerateAttackActions` から `baseEval` 引数を削除
- `enumerateCompactActions` を追加。圧縮列挙 (下記)
- `enumerateAllActions` / `enumerateAllPlays` / `enumerateAllCoreMoves` を追加。件数の上限なしの全合法手 (学習・Playmat の既定)
- `playOptions` / `markRepetitions` / `enumerateRawActions` を `internal` 化

## 価値ネットワーク (`valuerl/src/GruValueNet.kt`)

- 純Kotlin実装。KANN・cinterop 不使用
- カードトークン (能力値16次元 + カード番号の埋め込み8次元) の列 → GRU(32) → 大域特徴(20次元)と結合 → Dense(32) → tanh → 出力 tanh。パラメータ 9,249個
- カード番号は効果と結び付くため入力に必須。番号をハッシュで256バケットに割り当て、バケットごとに8次元の学習可能な埋め込みを持つ (語彙ファイル不要)
- トークン順は「相手トラッシュ → 自分トラッシュ → 相手の場 → 自分の場 → 手札」(変化しにくい順)。1手先読みの候補間で GRU の接頭辞計算をキャッシュ共有
- 学習は手書きの完全BPTT (系列長の上限なし) + Adam (勾配ノルムクリップ)。selftest で数値微分と照合
- 旧実装 (KANN の展開BPTT) にあった展開長上限・約17〜19回目のクラッシュは、C側グラフに依存しないため発生しない

特徴量 (`Features.kt`) は評価する側の視点で、非公開情報 (相手手札の中身・デッキの中身) は使わない。

## 方策 (`Agents.kt`)

- `NetAgent`: 全候補 + ステップ終了の afterstate を同じネットで評価し最大を選ぶ (1手先読み)。決着する手は ±1。ε-greedy / ソフトマックス探索
- `RandomAgent`: 一様ランダム (評価を持たない基準。進捗確認用)
- 選べる手が1つだけの局面は評価せず自動で進め、学習データにも入れない (autoPass 相当)

## 選択肢の列挙 (`--actions`)

| 値 | 内容 | メインステップの候補数 |
|---|---|---|
| `level` (既定) | Lv単位の正規形列挙 (`engine/src/LevelActions.kt`)。プレイはリザーブ払い・Lvの境目の配置 (ソウルコア有無)。コアは「カードのLv変更 (Lvの境目の数にする)」と「ソウルコア移動」を、各対象についてプレイ等の間に1回 (`GameState.coreWindow`)。学習と Playmat の既定 | 平均 6.0 (selftest 144局面) |
| `all` | すべての合法手 (`enumerateAllActions`)。プレイは支払い・配置の全組み合わせ (`enumerateAllPlays`)、コアは一括再配置 (最終配置を1手で選ぶ、`Rearrange.kt`) | 平均 1,223 (場所5以下の130局面)。終盤は爆発 (下記) |
| `compact` | 圧縮列挙 (下記) | 平均 7.9 |
| `full` | 従来の全列挙 (`enumerateActions`)。件数に上限あり (プレイは配置数ごとに50、コア移動は120) | 平均 125 |

- `level` で到達できる結果: 各カードのLv (またはスピリットの消滅) とソウルコアの場所の全組み合わせ。Lvの境目を超える余りのコアはリザーブに置く形にそろえる (カード効果が未実装の今のルールでは、カード上の余りのコアとリザーブのコアは支払い・Lvアップ・破壊時の行き先のどれでも同じ扱い)
- 同じ対象を続けて動かさないので、コアを動かし続ける循環は構造上起きない (selftest で、プレイ等の間に続くコアの手が「カード数+1」以下であることを確認)
- `all` の一括再配置の数は N×C(X+N−2, N−1) (N = リザーブ+カード数、X = コア数)。場所7・コア12で86,632通り、場所8・コア15で930,240通り。学習に使うとメモリ13GB超・極端に遅くなったため既定から外した
- 通常コアはどれも同じなので、同じ供給元の内訳で「どの分を配置に回すか」だけが違う手は1つにまとめる (盤面の結果が同じ)

### 圧縮選択肢列挙 (`engine/src/CompactActions.kt`)

bs.quensonwai.com の「エンティティ単位の可否 (controls) + パラメータ既定値 (支払い・再配置)」という設計を参考にした再実装 (コード流用なし)。

- プレイ: 支払いはリザーブの通常コア優先 → 不足時のみソウルコアの1通り。配置コアは Lv の境目 + ソウルコア有無のみ。リザーブで払えない場合は従来列挙から最大4手
- コア移動: 次Lvへ上げる / 余剰をリザーブへ / Lv1まで下げる / ソウルコア載せ替え / 余剰カードから他カードへ、の再配置のみ
- アタック・ブロック: 従来列挙のまま
- 適用は従来の `applyAction` と同一形式。メインステップの平均候補数 44.2 → 7.2 (selftest 実測)
- 全合法手の部分集合なので、フィールドのコアを使う召喚などは大幅に減る

## 学習 (`Trainer.kt`)

1. 現行ネット同士で自己対戦 (ε を 0.15 → 0.03 に線形減衰) し、各プレイヤーの afterstate 列を記録
2. λ-return: `G_{T-1} = z`, `G_t = (1-λ)·V(s_{t+1}) + λ·G_{t+1}` (z = 勝+1 / 負-1 / 手数上限の引分は −min(`--draw-max`, `--draw-base` + `--draw-gain` × 引分率)。引分率は自己対戦1局ごとの指数移動平均 (係数 `--draw-ema`)。引き分けが増えるほど罰が強くなる)
   - 引分を0にすると自己対戦が引き延ばし(手数上限到達)へ寄るため、両者に小さな負の報酬を与える
3. その反復の全局面を入れたバッファから一様サンプリングしたミニバッチで `0.5(V-G)²` を最小化し、更新後にバッファを空にする (目標値は作ったネットワークでのみ使い、ネットワークをまたいで持ち越さない)
4. `--eval-every` ごとに評価 (`Trainer.evaluate`、評価対局は ε=0、序盤 `--eval-temp-turns` ターンだけ温度 `--eval-temp` のソフトマックス、以降は貪欲):
   - 直接対戦: 現行 vs 最良を `--eval-games` 局 (先後交互)。スコア = (勝 + 0.5×分) / 局数
   - リーグ戦 (`League.kt`): 歴代最良モデル + ランダム方策 (Elo 0 固定の基準) の全員と `--league-games` 局ずつ対戦し、メンバーの Elo を固定したまま現行の Elo を最尤推定 (全勝・全敗で発散しないよう相手ごとに仮想0.5勝/1局を加算)
   - **最良の更新条件**: 直接対戦スコア ≥ `--promote` (既定55%) **かつ** リーグ Elo > 最良の Elo。直接対戦が全局引き分け (決着0局) のときは判定材料がないため、Elo の条件だけで判定
   - 更新したモデルは `<base>-p<番号>.gru.weight` に保存してリーグに加入 (上限 `--league-size`、超えたらランダム以外の最古を除外)。メンバーの Elo は加入時の値で固定
   - `--pool a,b,...` で既存の重みを開始時にリーグへ加入させられる
   - 注意: ランダム方策への全勝で付く Elo の上限は対戦数で決まる (10局なら約529)。リーグが育つほど上の値が付く

## ビルド・実行

```
mise run build-release        # Release ビルド (Debug の約7倍速: 同条件 90.7秒 → 12.4秒)
mise run build-release --dir build-tools   # 学習中で build-release の exe がロックされているとき
mise run selftest
mise run train-init           # 初期学習 → weights/init_g1.gru.weight, init_g1-best.gru.weight
mise run train --resume init_g1-best.gru.weight            # 続きから → weights/g1_g2.gru.weight
mise run train --depth 2 --resume init_h1-best.gru.weight  # 2手先までのミニマックスで学習
mise run train --depth 2 -- --lr 0.0005                    # -- 以降はそのまま train の引数
mise run match --a h1_h2-best.gru.weight --b init_h1-best.gru.weight --depth 2 -- --log match.log
mise run match --a h1_h2-best.gru.weight --b h1_h2-best.gru.weight --depth-a 3 --depth-b 1
mise run league --models a.gru.weight,b.gru.weight,c.gru.weight --games 20 --depth 2
mise run depthbench --max-depth 4   # 深さ1..Nの1手あたりの思考時間を同じ局面群で計測
mise run inspect -- init_g1.gru.weight
mise run value -- init_g1.gru.weight
```

`mise run <タスク> --help` で各タスクの引数を表示。

### 探索の深さ N (`--depth`)

- 選択肢を N 手先までミニマックス (αβ枝刈り) で読み、末端の局面を価値ネットで評価する。N=1 は従来の1手先読み
- 深さは「選択の余地がある局面」の数。自分が選ぶ局面は最大、相手が選ぶ局面は最小を取る。選択肢が1つの局面は数えずに進める
- 末端は「その手を選んだプレイヤーの視点」で評価し、相手視点なら符号を反転する (学習データと同じ視点)
- 学習 (N≥2) では λ-return の「次の局面の評価値」に対局中の探索値を使う
- 思考時間の実測 (init_h1-best、155局面): N=1 1.5ms / N=2 15ms / N=3 0.2秒 / N=4 2.2秒 (1手あたり平均)。1つ深くするごとに約10倍

### 探索の幅 k (`--width`)

- 0 (既定) は制限なし。k を指定すると、深さ2以上の各局面で1手先読みの値の上位 k 手だけを深く読む (ビーム探索)
- ルートで読まなかった手の値は、1手先読みの値を「読んだ手の最小値」未満に切り詰める (深さの違う値を比べて読んでいない手を選ばないため)
- 上位の外の良い手は読み落とす。`mise run depthbench --widths 0,3,5` で時間と「幅制限なしと同じ手を選ぶ割合」を比較できる
- 実測 (155局面、1手あたり平均 / 幅制限なしと一致): 幅3 深さ3 20ms / 68%、深さ4 72ms / 66%。幅5 深さ3 48ms / 83%、深さ4 221ms / 91%

### 主なオプション (`train`)

| オプション | 既定 | 内容 |
|---|---|---|
| `--iters` | 50 | 反復数 |
| `--games` | 16 | 1反復あたりの自己対戦数 |
| `--lambda` | 0.7 | TD(λ) の λ |
| `--lr` | 0.001 | Adam 学習率 |
| `--updates` / `--batch` | 200 / 32 | 1反復あたりの更新回数 / ミニバッチ |
| `--eps-start` / `--eps-end` | 0.15 / 0.03 | 探索率 |
| `--eval-every` / `--eval-games` / `--promote` | 5 / 20 / 0.55 | 対戦評価 |
| `--draw-base` / `--draw-gain` / `--draw-max` / `--draw-ema` | 0.1 / 0.8 / 0.9 / 0.1 | 手数上限引分の報酬 = −min(max, base + gain × 引分率)。引分率の移動平均係数 |
| `--step-penalty` | 0 | 1手ごとの報酬 −c (選択の余地があった自分の手1回につき)。長い引き分けの目標値が −1 に切り詰められ負けと同じになる副作用があるため既定は無効 |
| `--threads` | 論理プロセッサ数 | 同時に指す局数 (自己対戦・評価対局・リーグ戦・match・league を並列化。結果はスレッド数によらず同じ) |
| `--self-temp` | 0 | 自己対戦のソフトマックス温度 (0 なら ε-greedy のみ) |
| `--eval-temp` / `--eval-temp-turns` | 0.05 / 4 | 評価対局の温度と、温度を使うターン数 (以降は貪欲)。毎局同じ指し回しになるのを防ぐ |
| `--depth` | 1 | ミニマックス探索の深さ N (自己対戦・評価対局・リーグ共通) |
| `--width` | 1 | 探索の幅 k (1=1手先読みで最善の1手だけ深く読む、0=制限なし)。各局面で深く読む手を1手先読みの上位 k 手に絞る |
| `--league-games` / `--league-size` | 10 / 8 | リーグの対戦数/メンバー、最大人数 |
| `--pool` | - | 開始時にリーグへ加入させる重み (カンマ区切り) |
| `--max-steps` | 1500 | 1局の手数上限 (Lv単位の列挙は1ターン約12手。デッキ切れの敗北は約70ターン) |
| `--hidden` / `--dense` | 32 / 32 | ネットワーク幅 (新規作成時のみ) |
| `--emb-buckets` / `--emb-dim` | 256 / 8 | カード番号の埋め込みのバケット数と次元 (新規作成時のみ) |
| `--resume` / `--out` | - | 初期重み / 出力名 |
| `--deck1` / `--deck2` | deck-kogyo.yaml | デッキ |
| `--actions` | level | 選択肢の列挙。`level` = Lv単位の正規形列挙 / `all` = すべての合法手 / `compact` = 圧縮列挙 / `full` = 件数上限ありの従来列挙 |

## 重みファイル

- 形式: 独自バイナリ (ヘッダ `BSVN` + version 2 + 次元・埋め込み設定 + float64配列)。BSTools の KANN 形式とは互換なし
- 埋め込みのない旧形式 (version 1) も読める。読込時に埋め込みを追加し、追加部分の入力重みを0にするため評価値は旧ネットワークと同一
- 命名: `weights/<派生元>_<派生先>.gru.weight`。`--out` 省略時は `--resume` の名前から自動採番 (`init_g1` / `init_g1-best` → `g1_g2` → …)。最良モデルは `-best`、リーグ加入スナップショットは `-p<番号>` 付き

## スライド

`slides/BSTool.html`: Slides アーティファクト「BSToolsCla 強化学習の仕様」を1ファイルにまとめたビューア (ブラウザで直接開ける、← → で移動)。

## Playmat (`playmat/`)

BSTools の Playmats と同じ画面・API で、評価に BSToolsCla の GRU 価値ネットワークを使う盤面 UI。

```
mise run build-playmat
mise run playmat                     # http://localhost:8090 (BSToolsCla フォルダで起動)
mise run playmat --weights h1_h2-best.gru.weight --depth 2
mise run playmat -- --weights-p1 g1_g2-best.gru.weight --weights-p2 init_g1-best.gru.weight --actions compact
```

- 評価方式は「GRU」と「評価なし」。重みは画面の設定から `weights/*.gru.weight` を player ごとに選べる
- 各選択肢の評価値は学習時と同じ方法 (N 手先までのミニマックス。N=1 は仮に指した後の局面の V、決着する手は ±1)
- 探索の深さ N は起動時の `--depth` (1〜4) か、画面の設定「探索の深さ」で変更 (全タブ共通)
- 探索の幅 k は起動時の `--width` (既定1、0=制限なし) か、画面の設定「探索の幅」で変更 (全タブ共通)。幅1で深さ1以外はエラー (選ぶ手が変わらないため)
- 候補は既定で Lv単位の正規形列挙 (学習の既定 `--actions level` と同じ)。カードのメニューに「Lv変更」「ソウルコアの移動」を出す。`--actions all` ですべての合法手 (一括再配置を含む)、`--actions compact` で圧縮列挙
- 「📋 全選択肢」タブ: 現局面の選択肢をすべて一覧にする (プレイ / 場所ごとのコア移動「自身から」「自身へ」「交換」/ ステップ終了)。行をクリックするとその手を指す
- 右の「選択可能なアクション」では、コア移動・交換は最初「🔷【コア移動】 (件数)」の1行だけを出し、クリックで開く
- 自動選択中は見出しに「選択肢数:x/X」(x = 評価済み、X = 現局面の全選択肢) を出す。深さ2以上では評価の途中経過を SSE で更新
- ポートは 8090 (BSTools の 8080 / 8082 と同時に起動できる)
- ゲームはタブごとに別 (セッション)。URL に `?session=<ID>` を付けると、そのゲームを複数タブで共有・観戦できる
- 戻る/進む (1手ずつ、最大500手) と、選択ログ (「選択ログ (JSON)」タブ) に対応。決着すると `temp/<デッキ1>-<デッキ2>.YYMMDD-HHMMSS.<セッション>.json` に保存
- P1自動/P2自動は、表示モードが相手の視点でも動く。「⏩/⏭」で手番が移った後も自動で指し始める

## カードデータ

カードの値 (コスト、軽減、Lv ごとのコア数・BP、系統、《真界放》、効果テキスト) は公式カードリストから生成した `engine/src/OfficialCards.kt` を使う。手で編集しない。

```
node engine/tools/fetch-official-cards.mjs   # decks/*.yaml のカードを公式サイトから取得して OfficialCards.kt を再生成
```

- シンボルは公式ページに文字情報がないため「カードの色のシンボル1個」としている (未確認)
- 効果は、ガリザロト (26RSD03-007) の Lv1-2 アタック時効果以外は未実装 (効果テキストはデータとして保持)
- `~/.bscards/yaml/<番号>.yaml` があればそちらを優先する


### 探索設定の検査と評価タスク

- 幅1は「1手先読みの1位の手だけを深く読む」ので、深さを上げても選ぶ手は深さ1と同じ。**`--width 1` のとき `--depth` が1以外ならエラー** (train / match / league / Playmat の設定画面と API)。深さ2以上で読むには `--width` を2以上 (または 0 = 制限なし) にする
- `mise run eval-d2w3 [--models a,b,c] [--games N]`: 深さ2・幅3で総当たり戦 (既定は g2_g3-p1/p2/p3 と g1_g2-best、各100局)。ログは `weights/eval-d2w3.log`

---
name: kann-es-training
description: >-
  BSTools内のKANN(cinterop連携したC言語NNライブラリ)を使ったGRU価値ネットワークの構築・
  ES(進化戦略)による自己対戦学習・GUI連携に関する知見。KANN/ES/進化戦略/自己対戦学習/
  重みファイル/評価方式/cinterop関連の作業で参照する。
---

# KANN + ES 自己対戦学習(BSTools)

BattleSpirits Kotlin/Nativeシミュレータで、KANN(attractivechaos/kann, MIT license)を
cinterop連携し、GRU価値ネットワークをES(進化戦略、(1+1)型山登り法)で自己対戦学習させる
一式。このセッションで確立した設計・既知の不具合・回避策をまとめる。

## アーキテクチャ

- ソース: [`model/cinterop/kann-src/`](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/cinterop/kann-src)
  (`kann.h/.c`, `kautodiff.h/.c`, `libkann.a`)
- cinterop設定: [`model/cinterop/kann.def`](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/cinterop/kann.def)
- Kotlin側: [`model/src/KannRnnEvaluator.kt`](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/KannRnnEvaluator.kt) — `KannValueNetwork`クラス
- グラフ構成: トークン入力(RNN_TOKEN_DIM=10)→GRU(隠れ16)→大域特徴(12次元)とconcat→
  Dense(16)→tanh→`kann_layer_cost`が内部でDense(1)+tanh(出力)を追加。パラメータ数1793個。
- ビルド: MinGW向けはLLVM付属clang(`-target x86_64-w64-mingw32`)でクロスコンパイルする。
  **バンドルされたGCCは`internal compiler error`でクラッシュするため使わない**。

## 既知の重大バグ: BPTT(trainStep)は実用不可

`kann_unroll_array`によるBPTT学習(`KannValueNetwork.trainStep`)には未解決の実装依存バグがある:
- 展開長9以上で確実にクラッシュ(→`MAX_TRAIN_UNROLL_LEN=8`で回避)
- `kann_delete_unrolled`を呼ぶと元の`ann`の重みノードも道連れに解放される(共有ポインタのため)
  →**展開ネットワークは削除せず使い回す**(`unrolledFor`でキャッシュ)
- 上記を全て回避しても、繰り返し呼び出すと約17〜19回目に確実にクラッシュ(未解決、原因は
  KANN側`kad_unroll_helper`の実装依存の不具合とみられる)

**結論: BPTT経路は使わない。** `evaluate()`(連続フィード方式、unroll不使用)は長さ30程度まで
実測で完全に安定しているため、学習は全てこちらの経路(ES)に寄せる。

## ES(進化戦略)設計

[`GameServer/src/KannEsTrainer.kt`](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/GameServer/src/KannEsTrainer.kt)。
(1+1)型山登り法:
1. チャンピオンの重み(`getWeights()`で取得したFloatArray)にガウスノイズを加えて候補を作る
2. 候補 vs チャンピオンで自己対戦(`--games`局、先手/後手を交互入替え)、勝率を測る
3. 勝率>50%なら候補を新チャンピオンに採用、そうでなければ棄却
4. これを`--generations`回繰り返す(1世代=1回の変異→評価→採否判定サイクル)

対局は「評価値greedy」(1手先読みで最良の手を選ぶ、`evaluateActionWithKann`)。
KANNの`ann`は単一の計算グラフを使い回すため、手番ごとに`setWeights()`で中身を差し替える。
マルチスレッド化はcinterop連携の脆弱性を考慮してあえて見送り、単一スレッドで実装している。

### 既知の落とし穴1: 手数上限不足で全局引き分け

`--max-steps`が短すぎる(実測80)と自己対戦が決着せず全局引き分けになり、引き分けは常に0.5点
なので**勝率が世代を問わず機械的にちょうど50.0%に固着**する(統計的ノイズではなく構造的な
バグ)。`--max-steps 300`(自作GRU版`Tuner.kt`と同じデフォルト)なら安定して決着する。
デフォルト値も300に修正済み。

### 既知の落とし穴2: 未学習初期重みの「常にパス」局所解

未学習の初期重みは高確率で「ステップ終了(END_STEP)」を他の全行動より常に高く評価してしまう
(実測でEND_STEPとの評価差が約0.25もある)。`sigma=0.05`程度の摂動ではこの差を覆せないため、
候補・チャンピオン双方が「常にパス」の固定方策に陥り、**重みを変異させても対局結果が一切
変わらなくなる**(sigma=0でも0.05でも毎世代ちょうど勝率50.0%に固着することを実測で確認、
落とし穴1と症状が似ているが原因は別)。

**対策**: `kad_srand(void*, uint64_t seed)`(kautodiff.h、KANNの重み初期化に使う大域RNGの
シードを直接制御できる)で複数の初期化を試し、「初期局面で何らかの行動がEND_STEPを上回る」
(`endStepMargin`が正)初期化を選んでから学習を始める。`findGoodInitNet(trySeeds, baseSeed)`
として実装済み、`--init-search`(既定30)で試行回数を調整できる。

## 検証結果(実績)

- 初期完走:
  `--kann-es-train --generations 30 --games 8 --sigma 0.05 --seed 1 --max-steps 300 --init-search 30`
  → クラッシュなく1分43秒で完走、30世代中10世代採用、勝率0%〜87.5%と正常にばらつく
  (`kann-es-final.bin`)。
## ウェイトファイルの固有名命名規則

すべてのウェイトファイルは以下のフォーマットで統一管理する:
`<派生元の名>_<派生先の名>.<NNタイプ>.weight`

- `<派生元の名>`: 起点となったモデル名（例: `init`, `run1`, `run2`, `run3`, `run4`）。
- `<派生先の名>`: 生成されたモデル名（例: `run1`, `run2`, `run3`, `run4`, `run5`）。
- `<NNタイプ>`: ネットワーク種別（KANN価値ネットワークは `kann`）。
- サイクルスナップショット: `<派生元の名>_<派生先の名>-c<サイクル番号>.<NNタイプ>.weight`（例: `run4_run5-c1.kann.weight`）。
- `--resume` に指定されたファイル名から派生元名と次の派生先名が自動抽出・自動採番され、`--out` を省略してもこの規則に従ったファイル名で自動保存される。

### 最強モデルの進化実績と対応表:
- `init_run1.kann.weight`: 未学習初期重み探索からの初期学習。
- `run1_run2.kann.weight`: `run1` ベース。初期モデルに対し勝ち越し。
- `run2_run3.kann.weight`: `run2` ベース。25世代中10世代採用。直接対戦で `run2` に対し 8勝2敗 (80.0%)。
- `run3_run4.kann.weight`: `run3` ベース。20世代中9世代採用。直接対戦で `run3` に対し **7勝3敗 (勝率70.0%)** (先手時5勝/5戦 100%, 後手時2勝/5戦 40%)。
- `w4_w5-c1.kann.weight` (`run4_run5-c1.kann.weight`): `w3_w4` ベース。第1サイクルで強化され、`w4_w5` に対して **13勝7敗 (勝率65.0%)**、`w2_w3` に対して **12勝8敗 (勝率60.0%)**、`w3_w4` と互角（10勝10敗 50.0%）の成績を収める現行最強モデル。

## CLIコマンド一覧(GameServer)

- `--kann-es-train`: ES学習。`--generations --games --sigma --seed --max-steps --out --init-search --resume [--cycles N] [--target-name <name>] [--nn-type <type>]`
  - `--out` 省略時は `<派生元>_<派生先>.<NNタイプ>.weight` 規則で自動採名。
  - `--cycles N` (既定1): 複数サイクルの連続強化学習を実行。サイクルごとにスナップショット（例: `<out>-c1.kann.weight`）を自動保存し、開始前重みとの10局対戦評価で向上度を即座に測定。
- `--kann-match`: 2つの重みファイル同士を対戦評価（対戦評価モード）。
  - 引数: `--p1-weights <f> --p2-weights <f> --games N --seed S --max-steps M [--fixed-turn] [--log <path>] [--no-log]`
  - デフォルトで先手・後手を1局ごとに交互に入れ替えて公平対戦。各局の決着ターン数・手数を表示。
  - デフォルトで全対戦の全手における「盤面状態・候補行動一覧・各行動のKANN評価値・ステップ終了評価・採択結果・適用効果」をログファイル（既定: `kann-match.log`、`--log` で変更可能、`--no-log` で無効化）に詳細記録。
  - 最終集計として、各モデルの総合勝率・先後別勝率（先手時/後手時）・平均決着ターン数・平均手数を完全出力（コンソールおよびログファイル末尾）。
  - 省略した側は `findGoodInitNet` で探索した未学習初期重みで代用。
- `--kann-inspect <path>`: 重みファイルの統計(パラメータ数・最小/最大/平均/標準偏差)を表示
- `--eval-kann --kann-weights <path>` (または `--kann-weights-p1`/`-p2`): 起動時にKANN評価
  モードへ切り替え
- `--kann-rnn-debug`: 未学習ネットワークでの局面評価値を表示(スモークテスト)
- `--kann-debug`: cinterop連携のスモークテスト(`kannSmokeTest()`)

すべて [`GameServer/src/Main.kt`](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/GameServer/src/Main.kt) に配線。

## クロスプラットフォームとmise運用の原則

- OS固有スクリプト（`.ps1`, `.bat`, `.sh` 等）をアドホックに作成して依存させることは**禁止**。
- 対戦評価や診断などのロジックは Kotlin/Native（`GameServer` 等）本体に直接実装し、全OS共通のCLIオプションとして提供する。
- タスク実行は [`mise.toml`](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/mise.toml) 内のタスク定義に集約する:
  - `mise run build`: Kotlin/Nativeバイナリ（GameServer）のビルド。
  - `mise run build`: Kotlin/Nativeバイナリ（GameServer）のビルド。
  - `mise run train`: 汎用ES強化学習タスク（引数追加可）。
  - `mise run train-w3`: `w2_w3.kann.weight` を起点に `w3_w4.kann.weight` を学習。
  - `mise run train-w4`: 現行最強 `w3_w4.kann.weight` を起点に `w4_w5.kann.weight` を学習。
  - `mise run inspect -- <file>`: 重みファイルの統計情報（パラメータ数、最小/最大/平均/標準偏差）を表示。
  - `mise run match-w3-w4`: `w3_w4` vs `w2_w3` の10局公平対戦評価。
  - `mise run match-w4-w5`: `w4_w5` vs `w3_w4` の10局公平対戦評価（`kann-match.log` 自動記録）。
  - `mise run train-loop`: 複数サイクル（既定3サイクル×20世代）の強化学習を連続実行（引数 `-- --cycles N` で回数変更可能）。
  - `mise run train-loop-pipeline`: ビルド → 複数サイクル強化学習 → 重み検査 → 最終対戦評価の一括パイプライン。

## GUI連携(Playmats)

- `EvalMode`に`KANN`を追加([`model/src/RnnEvaluator.kt`](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/RnnEvaluator.kt))。
  `LINEAR`/`RNN`/`KANN`を切替可能。
- **player1/player2で別々の重みファイルを指定できる**: `rnnParamsP1/P2`・`kannWeightsP1/P2`
  という新グローバルを追加し、`syncEvalForTurn(state)`が`state.choosingPlayerId`に応じて
  「現在有効な」重み(`rnnParams`の中身、または`kannNet.setWeights()`)を自動的に差し替える。
  `enumerateActions`([`model/src/Rules.kt`](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/Rules.kt))と
  `stepEndEval`(RnnEvaluator.kt)の**両方**の冒頭で呼ぶ(選択肢とステップ終了の基準値は同じ
  評価器でなければ比較が成立しないため、過去に片方だけ更新し忘れるバグが実際に起きた教訓が
  ある — 二度と複製しないこと)。片方のプレイヤーだけ重みを指定した場合、もう片方の手番では
  直前に有効だった重みがそのまま使われる(実質共有にフォールバック)。
- `POST /api/eval-config`(GameServer・Playmats両方に実装):
  `{"mode":"KANN","weightsPathP1":"...","weightsPathP2":"..."}`。Playmatsはローカル評価にも
  適用しつつGameServerへ中継する(`httpPostJson`)。
- `GET /api/weight-files`([`model/src/RnnEvaluator.kt`](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/RnnEvaluator.kt)の`listWeightFiles`):
  作業ディレクトリ直下の `.weight`, `.bin`, `.pb` ファイルを列挙して返却。
- Playmats WebUI([`Playmats/index.html`](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/index.html))
  に`<dialog id="settings-dialog">`の設定ダイアログを実装済み。
  - フォーマット・デッキ・シード・表示モード・GameServer URL・評価方式を統合指定。
  - **重みファイルの直接指定**: player1/player2 の重みファイル入力欄は「自由テキスト入力（datalistサジェスト付）＋一覧ドロップダウン選択」のハイブリッドUIを採用。任意のカスタムパスの直接タイピングと、サーバーから列挙されたファイル一覧からのクリック選択の双方に対応。入力値は即座に `localStorage` に保持され、サーバーへ自動反映される。
  - **自動実行ボタン**: ターン・ステップ表示エリアに3つの自動進行ボタンを実装。
    1. `⚡ 次の選択を自動で行う`: 現局面で評価値最良の手を1手だけ即時実行。
    2. `⏭ ステップ終了まで自動で行う`: 現在のフェイズ/ステップが切り替わるまで自動進行。実行中は「⏹ 停止」ボタンとなり中断可能。
    3. `⏩ ターン終了まで自動で行う`: 手番ターンが切り替わるまで自動進行。実行中は「⏹ 停止」ボタンとなり中断可能。
  - ツールバーは「⚙ 設定」ボタン+操作ボタン(新規/リスタート/戻る/進む)のみに簡素化。

## 共通の重複排除の原則

このコードベースには「選択肢の評価とステップ終了の基準値は同じ評価器で計算しなければ
比較が成立しない」という制約があり、過去に実際にPlaymats側だけ実装を複製して更新し忘れる
バグを起こしている(`stepEndEval`のコメント参照)。KANN関連の評価ロジック
(`evaluateActionWithKann`/`kannStepEndEval`/`syncEvalForTurn`)は**必ず`model/src/`に
1箇所だけ実装し**、GameServer/Playmatsからはimportして使う。GameServerの`KannEsTrainer.kt`
も自前で重複定義していたものを、後から`model`側の共通実装を使うようリファクタ済み。

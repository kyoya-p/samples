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
- 最強モデルの進化実績:
  - `kann-es-run2.bin`: `final` および `final-v2` に対し勝ち越し。
  - `kann-es-run3.bin`: `run2` をベース(`--resume kann-es-run2.bin --sigma 0.04 --generations 25 --games 12`)に強化学習。25世代中10世代採用。
  - 直接対戦評価: `run3` vs `run2` (往復40局) で **36勝4敗 (勝率90.0%)**、公平交互10局で **8勝2敗 (80.0%)**。未学習初期重みに対して **20戦20勝 (100.0%)**。現行最強モデル。

## CLIコマンド一覧(GameServer)

- `--kann-es-train`: ES学習。`--generations --games --sigma --seed --max-steps --out --init-search --resume`
- `--kann-match`: 2つの重みファイル同士を対戦評価（対戦評価モード）。
  - 引数: `--p1-weights <f> --p2-weights <f> --games N --seed S --max-steps M [--fixed-turn]`
  - デフォルトで先手・後手を1局ごとに交互に入れ替えて公平対戦。各局の決着ターン数・手数を表示。
  - 最終集計として、各モデルの総合勝率・先後別勝率（先手時/後手時）・平均決着ターン数・平均手数を完全出力。
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
- タスク実行は [`mise.toml`](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/mise.toml) 内のタスク定義（`mise run match` や `mise run match-run2-run3` 等）で直接バイナリまたは `kotlin run` を呼び出す形に集約する。

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
- Playmats WebUI([`Playmats/src/index.html`](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/src/index.html))
  に`<dialog id="settings-dialog">`の設定ダイアログを実装済み。フォーマット・デッキ・シード・
  表示モード・GameServer URL・評価方式・player1/player2の重みファイルをまとめて指定できる。
  ツールバーは「⚙ 設定」ボタン+操作ボタン(新規/リスタート/戻る/進む)のみに簡素化。

## 共通の重複排除の原則

このコードベースには「選択肢の評価とステップ終了の基準値は同じ評価器で計算しなければ
比較が成立しない」という制約があり、過去に実際にPlaymats側だけ実装を複製して更新し忘れる
バグを起こしている(`stepEndEval`のコメント参照)。KANN関連の評価ロジック
(`evaluateActionWithKann`/`kannStepEndEval`/`syncEvalForTurn`)は**必ず`model/src/`に
1箇所だけ実装し**、GameServer/Playmatsからはimportして使う。GameServerの`KannEsTrainer.kt`
も自前で重複定義していたものを、後から`model`側の共通実装を使うようリファクタ済み。

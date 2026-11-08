---
name: nano-translator
description: >-
  Chrome 拡張 Nano Translator (Gemini Nano / Prompt API によるページ翻訳・画像解析) の構成、設定キー、
  実機 (Gemini Nano) での検証手順、既知のモデル挙動と落とし穴。GeminiNano/Translator の修正・検証時に使用する。
---

> 🤖[AI 生成 (Claude)。2026-11-08 のセッションで確認した事実のみ記載]

# Nano Translator 開発・検証スキル

## 構成
- [content.js](~/projs/samples/2026/GeminiNano/Translator/content.js): ページ翻訳本体 (注入型 content script)
  - `collectVisible`: 翻訳単位の収集。表示中のテキストを優先し、表示中が無いときだけ画面外を距離順に足す。`units.visible` で表示中の有無
  - `serialize` / `build`: インライン要素を目印 `<n>…</n>` / `<n/>` にして 1 回で翻訳し、検証して DOM に戻す。`normMarks` で表記ゆれ (`<1 />`、全角) を正規化、`stripMarks` で残った目印を除去
  - `translateBatch` / `translateMany`: 複数単位を JSON 文字列配列でまとめ翻訳 (原文合計 `maxUnit` 文字まで)。失敗した単位は `translateUnit` で単独翻訳
  - `translateOne`: 単独翻訳。訳文に原文に無い文字体系 (キリル等) が混ざれば 1 回訳し直す (`contaminated`)
  - `analyzeVisibleImages`: 画像解析 (スクリーンショット方式)。表示中テキストの翻訳後に実行。画像内の文字の翻訳は通常のテキスト翻訳で行う
  - `imgTip` (画像のホバー表示)、`imgMarks` (画像の解析待ち=黄 / 解析中=緑の枠)、`live` (デバッグ時のリアルタイム表示)、`showTip` (翻訳ブロック詳細ダイアログ)
- [prompt.js](~/projs/samples/2026/GeminiNano/Translator/prompt.js): セッション生成、`wrap` / `wrapBatch` / `parseBatch`、辞書 (`cache`)、コード判定の採点 (`codeScore` / `codeRegex`)
- [background.js](~/projs/samples/2026/GeminiNano/Translator/background.js): 注入、`capture` (captureVisibleTab)、offscreen 準備
- [options.html](~/projs/samples/2026/GeminiNano/Translator/options.html) / options.js: 設定ページ (コード判定の試算つき)

## 設定キー (chrome.storage.local)
- `skipCode` (既定 ON)、`markUntranslated` (ON)、`markCode` (OFF)、`debugCode` (OFF)、`analyzeImages` (ON)
- `maxUnit` (1 回に AI で翻訳する最大文字数、既定 2000)、`cacheMax`、`cacheMaxLen`、`codeParams` (既定値との差分のみ)
- 辞書キー: `tr2:<hash>` (画像の結果は原文 `[image] URL`)

## 実機検証手順 (Windows、Chrome 156 で確認)
1. 拡張のコピーを作り `manifest.json` に `"host_permissions": ["<all_urls>"]` を追加 (自動操作から注入・撮影するため。原本は変更しない)
2. Playwright (`npm i playwright`、ブラウザ本体はダウンロード不要、`channel: 'chrome'`) で永続プロファイルを起動
   - `ignoreDefaultArgs: ['--disable-extensions']` 必須 (既定で拡張が無効化される)
   - `--load-extension` は無視される → `--enable-unsafe-extension-debugging` を付け、CDP `Extensions.loadUnpacked` で読み込む
   - service worker は `chrome.storage` が使えるまで待ってから操作 (直後は `chrome is not defined`)
   - 拡張ページへの `page.goto` は `ERR_BLOCKED_BY_CLIENT`。操作は service worker の `evaluate` で行う
3. モデル取得: 新規プロファイルは `availability` が `downloadable`。ユーザー操作扱い (`Runtime.evaluate` の `userGesture: true`) で `LanguageModel.create` を呼ぶと取得開始。約 17 分 (約 2.8GB、進捗 90% で一時停止後に完了)
4. 別プロファイルへは `OptGuideOnDeviceModel/` と `Local State` をコピーすればダウンロード不要
5. テストページ: `npx http-server -p 8765` で文字入り画像 (img: SVG / PNG / JPEG、インライン svg、canvas、文字なし) を配信。`localhost` はセキュアコンテキストなので Prompt API が使える
6. 証跡: 各画像にホバーした状態と 15 秒ごとの画面をスクリーンショットで保存。期待語 (例 `/STOP|Slow/`) で一致判定

## 実機 Gemini Nano の既知の挙動
- 速度: 単独翻訳 1 件約 6 秒、まとめ翻訳 (約 2000 文字) 1 分以上、画像 1 枚 20〜85 秒 (環境による)
- まとめ翻訳に `responseConstraint` を付けると先頭要素が未翻訳で返ることがある → 付けない
- 画像入力で `UnknownError: kErrorUnknown` が不定期に発生 (約 4 回に 1 回) → 1 回再試行、150 秒で打ち切り
- 画像入力で翻訳させると空欄・逆の意味の誤訳 → 画像内の文字だけ読ませ、翻訳はテキスト翻訳で行う
- 訳文・説明にキリル文字が混入 (「ようこそ лаборатории へ」) → 検出して訳し直す
- 目印の表記ゆれ (`<1 />`、`＜１／＞`)、原文に無い目印の付加 → 正規化・除去
- 文字なし画像で文字をでっち上げることがある (例 `@npngy/landscape`)、行の読み落としもある (未対処)

## GPU / CPU の確認 (2026-11-08 実機: Chrome 156、GeForce GT 730)
- 公式の要件: GPU は VRAM 4GB 超、CPU は RAM 16GB 以上かつ 4 コア以上 (Chrome 140 から CPU 対応)。音声入力は GPU 必須
- 実機の判定値 (Broker State): 必要 VRAM 3000 MiB (公式文書は 4GB 超)、インストール時の空き容量 20480 MiB (公式文書は 22GB)、保持に 5120 MiB。文書と実機で値が異なる
- 確認先: `chrome://on-device-internals`
  - Broker State: `Performance Class` = `GpuBlocked`、`Enough VRAM` = false (0 MiB 実測 / 3000 MiB 必要)、`Device Capable` = true
  - Tools: `Device performance class` = Very Low
  - Event Logs: 翻訳ごとに LanguageModel セッションの生成・実行・破棄が記録される (GPU/CPU の直接表示は無い)
- `chrome://gpu`: 描画は Hardware accelerated。`Driver D3D12 feature level` = D3D 11.0、Vulkan 非対応、Dawn は `D3D11 backend (Compatibility Mode)` で WebGPU が Blocklisted (`D3D11 backend not fully implemented`)
- 判断: この環境の翻訳は GPU を使っていない (GpuBlocked + モデルは実行できている)。CPU 推論は状況からの判断で、直接の表示は未確認
- 未確認: Dawn のブロックと Gemini Nano の GPU 判定の関係、crbug.com/41479539 の内容、フラグ `Enabled BypassPerfRequirement` の効果
- 確認時のメモ: `chrome://gpu` の全文はページ内クリック後に Ctrl+A / Ctrl+C で取得できた。タスクマネージャーの GPU 3D 使用率は Chrome 以外の描画も含むため根拠にならない

## 落とし穴
- スクリーンショット方式は画像に重なる要素も写る (ページ全体のヒーロー画像など)
- マウスを動かさずにスクロールすると下の画像が変わる → スクロール時に再判定 (対処済み)
- モデルを読み込んだ Chrome を多数開くとメモリ不足になり、Claude Code がバックグラウンドのシェル (画像サーバ等) を停止する
- 生成スクリプトの出力 (パネル HTML 等) に AI マークを埋め込まない

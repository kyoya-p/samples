# Tab Transcriber (ChromeItr)

> 🤖[AI 生成 (Claude)。2026-11-12。実機での音声取得・文字起こしは未検証]

# 概要

特定のタブの音声を取得し、そのページ上のダイアログに文字起こしを表示する Chrome 拡張機能。

- 音声取得: `chrome.tabCapture` (拡張機能アイコンのクリックで対象タブを開始/停止)
- 文字起こし: Gemini Nano (Prompt API の音声入力) を優先し、使えなければ Whisper (transformers.js、ブラウザ内実行) へ自動切替
- 外部送信なし (Whisper のモデルは初回のみ Hugging Face から取得してキャッシュ)
- 翻訳: 既定で日本語へ翻訳 (オプションで翻訳先を固定指定。翻訳元と同じ言語なら翻訳しない)。原文は小さく併記
- 表示: ページ内の Shadow DOM ダイアログ (ドラッグ移動・最小化・コピー・消去・停止)

# 構成

| ファイル | 役割 |
| --- | --- |
| `manifest.json` | MV3。権限 `tabCapture` `offscreen` `activeTab` `scripting` `storage`。CSP は `'wasm-unsafe-eval'` |
| `background.js` | アイコンクリックで開始/停止。offscreen の生成、`getMediaStreamId`、メッセージ中継、バッジ表示 |
| `offscreen.js` / `.html` | `getUserMedia` (tab) で音声取得 → 16kHz で無音区切り → エンジンへ。タブ音は `AudioContext` で再生に戻す |
| `worklet.js` | AudioWorklet。100ms ごとに PCM をメインへ渡す |
| `engines.js` | `NanoEngine` / `WhisperEngine` / transformers.js の共通ローダ |
| `translate.js` | 翻訳。Chrome Translator (取得済みの対のみ) → ローカルモデル (ja→en・zh→en は opus-mt、それ以外は NLLB-200) |
| `content.js` | 対象ページ上のダイアログ |
| `options.html` / `.js` | エンジン・言語・Whisper モデルの設定 |
| `scripts/build.mjs` / `mise.toml` | esbuild で transformers.js を onnxruntime-web ごと `lib/transformers.bundle.js` にバンドルし、wasm と共に `lib/` へ配置 |

# インストール

1. `mise run build` (依存導入と `lib/` のバンドル。`lib/` は約 27MB で git 管理外)
2. `chrome://extensions` (Edge は `edge://extensions`) → デベロッパー モード ON → 「パッケージ化されていない拡張機能を読み込む」でこのディレクトリを選択
3. 必要なら拡張機能の「オプション」でエンジン・音声の言語 (固定 / 自動判定)・翻訳先 (日本語 / English / 翻訳しない)・Whisper モデルを設定

# 使い方

1. 文字起こししたいタブを開き、拡張機能アイコンをクリック → 右下にダイアログが出る
2. 音声が出ると区切りごとに行が追加される (無音 0.7 秒で区切り、最長 15 秒)
3. ダイアログ上段の「音声」(認識言語: 自動判定 / 日本語 / English 等) と「翻訳」(日本語 / English / しない) で切替。変更は保存され、実行中なら次の区間から反映 (2026-11-12 実画面で確認)
4. ダイアログの「停止」またはアイコンの再クリックで停止。停止後は「閉じる」でダイアログを消す

# エンジン

- 自動 (初期値): Gemini Nano → 初期化または実行に失敗したら Whisper。切替はダイアログ下部のステータスに表示
- Gemini Nano: 公式ドキュメント ([Prompt API](https://developer.chrome.com/docs/ai/prompt-api)) の記載。拡張機能は Chrome 138 以降、音声入力は GPU 必須 (VRAM 4GB 超)、空き 22GB 以上
- Whisper: 初期モデル `onnx-community/whisper-base`。WebGPU があれば GPU、なければ CPU (WASM)

# 検証状況

- 確認済み (2026-11-12):
  - 全 JS の構文 (`node --check`)、manifest の JSON
  - ダイアログ UI の各状態 (開始・取得中・文字起こし 12 行・最小化・停止・エラー) を Playwright で画面確認 (chrome API はスタブ)
  - 拡張機能として Chromium に読込み、service worker 起動とエラーなしを確認
  - 拡張機能ページ (offscreen.html) で Whisper (`onnx-community/whisper-tiny`、wasm) の初期化 (約 8 秒) と推論の実行を確認。テスト用 Chromium では Gemini Nano は `availability=unavailable` で、Whisper への切替経路の前提を確認
  - 無音入力で Whisper が同一語句を反復する幻覚を起こしたため、`max_new_tokens: 200` と反復圧縮 (`collapseRepeats`) を追加し再テストで圧縮を確認
- 未検証 (実機で確認が必要):
  - `tabCapture` による音声取得と再生の継続
  - 実在の音声での文字起こし精度 (テストは無音入力のみ)
  - 実際の Chrome での Gemini Nano (音声入力) の可否。offscreen での記載は公式ページになし
  - WebGPU での Whisper 実行
  - 無音しきい値 (`SPEECH_RMS = 0.004`) の妥当性

- 実音源テスト (2026-11-12): `Documents/Rec` の録画 (約 99 秒、英語の発話) をタブで再生し、アイコン操作相当で開始。Whisper-base (wasm) の文字起こしがダイアログに表示された。言語を `ja` 固定にすると日本語の誤認識になり、`auto` で英語の文字起こしになった。このため言語の既定を自動判定に変更。連続発話では 15 秒で強制区切りとなり、CPU (wasm) の base モデルでは処理が遅れて取りこぼしと遅延があった (WebGPU 実行は未検証)

# 翻訳

- 実音源での通し検証 (2026-11-12): 英語音声の録画 (約 99 秒) をタブで再生し、Whisper-base と NLLB を同一ページで動作させ、ダイアログに日本語訳 (大) と原文 (小) を表示できた (例: `I'm sorry.` → 申し訳ない)。`bad_alloc` は発生せず。ただし Whisper-base (wasm) の出力は短く疎で、同じ語句の重複 (`I'm sorry.` が 2 回) もあった。認識精度は Whisper モデルのサイズ (small など) と WebGPU の有無に依存し、未評価

- 設定: オプションの「翻訳先 (固定)」。既定は日本語。「音声の言語」を固定にするとそれを翻訳元として使い、自動判定のときは `LanguageDetector` (取得済みの場合) または文字種で判定
- 順序: ① Chrome 組み込み Translator (`availability` が `available` の対のみ) → ② ローカルモデル (初回のみ Hugging Face から取得してキャッシュ)
- ローカルモデル: ja→en と zh→en は `Xenova/opus-mt-*`、それ以外 (en→ja を含む) は `Xenova/nllb-200-distilled-600M` (日・英・中・韓・西・仏・独の相互)。en→ja を opus-mt-en-jap にしたところ「わたし は, あなた を 鎖 に 染ま ら せ,」のような意味不明な出力 (実測) になったため NLLB に変更。NLLB は約 600MB、初回ロード約 40 秒、1 文 約 4 秒 (wasm)
- Chrome Translator は、テスト用 Chromium (Chrome 151) で `availability=downloadable` の対に `create()` するとタブがクラッシュしたため、取得済みの対だけ使う。取得済みの場合の動作は未検証

# 日本語音声 (2026-11-12 検証)

- Windows 音声合成 (Haruka、日本語 4 文) をタブで再生して確認
- 音声の言語「日本語」固定: 4 文とも日本語で認識 (誤り: 「お集まり」→「集まり」、「仕様」→「使用」)。ja→en 翻訳も表示
- 音声の言語「自動判定」: Whisper-base が日本語音声を英語で出力した。日本語の音声は「日本語」固定を推奨
- 修正: 翻訳を別キュー化 (翻訳モデル取得中も原文を即表示、区間破棄を防止)。停止時の offscreen 即時終了で末尾の区間が消えたため、処理完了後に閉じるよう変更

# 既知の制約・メモ

- 1 タブの音声はキャプチャ中、拡張機能経由で再生される (音量は元のタブ設定に依存)
- Nano / Whisper とも区切りごとの処理のため、表示は発話から数秒遅れる
- ページ遷移 (`loading`) とタブを閉じたときは自動停止

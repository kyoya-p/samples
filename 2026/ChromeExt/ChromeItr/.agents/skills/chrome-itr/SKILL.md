---
name: chrome-itr
description: >-
  Chrome 拡張 ChromeItr (Tab Transcriber): tabCapture でタブ音声を取得し、Gemini Nano / Whisper (transformers.js) で
  文字起こししてページ上のダイアログに表示する。構成、MV3 での落とし穴、検証手順。ChromeItr の修正・検証時に使用する。
---

> 🤖[AI 生成 (Claude)。2026-11-12 のセッションで確認した事実と、未検証の事項を区別して記載]

# 構成
README.md の「構成」を参照 (background → offscreen → engines → background 経由で content へ)。

# 確認済みの事実
- Prompt API (公式 https://developer.chrome.com/docs/ai/prompt-api、2026-10-21 更新): 音声は `{type:'audio', value: AudioBuffer|ArrayBuffer|Blob}`、`expectedInputs` に `{type:'audio'}`、拡張機能は Chrome 138 以降、音声入力は GPU 必須。offscreen での可否は記載なし
- transformers.js 4.3.2 (`dist/transformers.web.js`) は wasm を既定で jsDelivr から取る。MV3 CSP では外部 CDN と blob: が使えないため、`env.useWasmCache=false` + `env.backends.onnx.wasm.wasmPaths = {mjs, wasm}` で同梱の `ort-wasm-simd-threaded.asyncify.{mjs,wasm}` (onnxruntime-web/dist) を指す (ソース transformers.web.js の `ensureWasmLoaded` を読んで確認。実測で動作確認済み、下記)
- `env.backends.onnx` は `ONNX_ENV` の浅いコピーで、`.wasm` は同一オブジェクトのため `env.backends.onnx.wasm.wasmPaths` の代入が効く (ソース確認)
- ダイアログ UI は chrome API をスタブした Playwright (chromium_headless_shell) で状態別に確認した

- 実測 (2026-11-12): Playwright + chromium-1234 (`--load-extension`、headless=new) で拡張機能ページから Whisper-tiny が wasm で初期化・推論できた。テスト用 Chromium の Gemini Nano は `availability=unavailable`

# 落とし穴
- `transformers.web*.js` は bare specifier `onnxruntime-web/webgpu` を import しており、拡張機能ページではそのまま読めない (`Failed to resolve module specifier`)。esbuild で `lib/transformers.bundle.js` にバンドルする (scripts/build.mjs)
- Whisper は無音・雑音で同一語句を反復する幻覚を起こす (実測)。`max_new_tokens` と反復圧縮で対処
- `{ target: 'content', ...msg }` は `msg.target` で上書きされる。`{ ...msg, target: 'content' }` の順にする
- `npm install` が長いとバックグラウンド化され、wasm のサイズが途中値になる。完了後に `mise run build` を再実行して確認する
- Bash のヒアドキュメントに日本語と `'` を含む長文を入れるとパースエラーになった。Write ツールで作成する

- ホスト要素に `all:initial` を付けると `hidden` 属性が効かない (UA の `[hidden]{display:none}` を上書き)。表示切替は `style.display` で行う (2026-11-12 「閉じるで閉じない」の原因)
- 言語を `ja` 固定にすると英語音声が誤認識になる。既定は `auto`

- 翻訳 (2026-11-12 実測、拡張機能ページ上): opus-mt-ja-en は良好。opus-mt-en-jap は意味不明で不可。NLLB-200-distilled-600M (q8、wasm) は en→ja 良好 (約 4 秒/文、初回 約 40 秒)
- Chrome Translator: テスト用 Chromium 151 で `availability=downloadable` の `create()` が **タブごとクラッシュ**。offscreen で起きると録音ごと落ちるため、`available` の対のみ使う
- `std::bad_alloc`: 同じ重いモデルを複数インスタンスでロードすると発生。モデルキャッシュはモジュール共有にする

- Whisper-base の言語自動判定は日本語音声 (TTS) を英語で出力した。日本語音声は language='ja' 固定が必要 (実測)
- 翻訳を文字起こしと同じキューで待つと、NLLB 取得 (約 80 秒) の間に原文も出ず区間が破棄された → 別キュー化
- 停止時に offscreen を即 close すると末尾区間が消える → offscreen の 'idle' 通知後に close
- 日本語音源のテストは Windows SAPI (Microsoft Haruka Desktop) で wav を生成して再生できる

# 未検証
- 実機の tabCapture、実音声での精度、実 Chrome での Gemini Nano、WebGPU、無音しきい値

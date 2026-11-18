---
name: chrome-doodle
description: >-
  Chrome 拡張 ChromeDoodle: Webページ上への直接描画・テキスト直接編集・スポイト・暗転クロップ・
  ページ全範囲スクロール撮影・Desktop/Window採取を行う MV3 拡張機能の構成、実装規約、落とし穴、検証手順。
  ChromeDoodle の修正・機能追加・デバッグ時に使用する。
---

# ChromeDoodle 運用・開発スキル

Webページ上に Shadow DOM で隔離された Canvas / ツールバーを重ねて注釈・編集・撮影を行う Manifest V3 Chrome 拡張機能の開発ガイド。

## ファイル構成
- [manifest.json](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/ChromeDoodle/manifest.json): MV3 定義（`activeTab`, `scripting`, `storage`, `clipboardWrite`, `clipboardRead`、ショートカット `Alt+Shift+D`）
- [background.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/ChromeDoodle/background.js):
  - アイコンクリック時のトグル／未注入タブへの動的 `executeScript`
  - `CHROME_DOODLE_CAPTURE_TAB`（`chrome.tabs.captureVisibleTab`）
  - `CHROME_DOODLE_GET_ZOOM` / `chrome.tabs.onZoomChange` → `CHROME_DOODLE_ZOOM_CHANGED`（タブのズーム倍率同期）
  - `CHROME_DOODLE_OPEN_DESKTOP_CAPTURE` / `CHROME_DOODLE_DESKTOP_RESULT`（フォールバック用ポップアップウィンドウ連携）
- [content.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/ChromeDoodle/content.js): Shadow DOM UI・Canvas 描画エンジン・図形ハンドル・スポイト・編集モード・クロップ・スクロール撮影・Undo/Redo・ズーム非連動 UI 制御
- [desktop_capture.html](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/ChromeDoodle/desktop_capture.html) / [desktop_capture.js](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/ChromeDoodle/desktop_capture.js): HTTP ページや `Permissions-Policy` で `getDisplayMedia` が拒否された場合に拡張機能オリジン上で画面採取を行うポップアップ
- [demo.html](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/ChromeDoodle/demo.html): 拡張未導入環境向けの単体テストページ
- [README.md](file:///C:/Users/kyoya/projs/samples/2026/ChromeExt/ChromeDoodle/README.md): 機能・ショートカット・利用手順

## 設計上の重要ポイント・規約

### 1. Shadow DOM 隔離と 4行固定ダイアログ型ツールバー・UI統一規約
- `#chrome-doodle-host` に `attachShadow({ mode: 'open' })` で UI・スタイルを完全隔離する。
- ツールバー（`.cd-toolbar`）は 4行（1: ツール10種、2: スポイト+9色+カスタム色、3: 太さ4種+塗り3種+フォントサイズ、4: 貼付/Undo/Redo/全消去/コピー/全範囲/Desk-Win/保存/閉じる）の固定カード形状（`flex-direction: column`、`flex-wrap: nowrap`）で表示し、ウィンドウ幅による崩れを防ぎつつレイアウトを勝手に作り変えない。
- 上部のドラッグハンドル要素は廃止し、ダイアログ全体のどこからでもドラッグ移動できる（`select` / `input[type="color"]` を除く。移動距離 4px 以上でドラッグ確定＋直後の `click` をキャプチャフェーズで抑止）。
- ダイアログ左側に上から下まで続く縦長の `.cd-toolbar-min`（グリップ SVG ハンドル。旧 `_` ボタンは廃止）を置く。`.cd-toolbar` は横並び（ハンドル + `.cd-toolbar-body` に 4 行を縦積み）。ハンドルをクリックすると最小化＝`.cd-toolbar-body` を隠してハンドルだけ残し、画面左端（`left:0`、`.minimized` は右側のみ角丸）へ移動。最小化中のハンドルは上下方向のみドラッグ移動可（`left:0` 固定、`clampToolbarToViewport` も同様）。再クリックで展開（左右は最小化前の位置、上下は最小化中に動かした高さを維持し、`clampToolbarToViewport` で画面内に収める）。ドラッグ移動は全面ドラッグと共通。
- **スタイル・配色を勝手に変えない**:
  - 下段アクションアイコンは全て同じ基本色（`#d4d4d8`）で統一し、`highlight` / `accent` などの個別色を付けない（選択状態 `.active` の `#6366f1` と `close-btn` のホバー赤のみ例外）。
  - `.cd-crop-actions` 等のフローティングUIも同じコンテナスタイル（`rgba(24, 24, 27, 0.92)`・`border-radius: 14px`・`backdrop-filter: blur(16px)`）・`.cd-action-icon-btn`・同一 SVG アイコンを共有し、独自デザインへ分岐させない。

### 2. ページズームと連動しないフローティング UI
- ページズーム倍率（Ctrl + `-`/`+`）は `background.js` の `chrome.tabs.getZoom` と `chrome.tabs.onZoomChange` で取得する（`window.devicePixelRatio` 単独では OS DPI 倍率と区別しづらいためタブズーム API を基準にする）。
- `content.js` の `applyPageZoom` で `#chrome-doodle-host` に CSS 変数 `--cd-inv-zoom: 1 / zoom` を設定し、`.cd-toolbar`, `.cd-crop-info`, `.cd-crop-actions`, `.cd-modal-card`, `.cd-toast` に `transform: scale(var(--cd-inv-zoom, 1))` を適用する。
- **ドラッグ・座標系**: 初期配置は上中央（`left: 50%; transform: translateX(-50%) scale(var(--cd-inv-zoom, 1)); transform-origin: top center`）。4px 以上のドラッグ開始時に `.positioned` クラスを付与して `left`/`top` 基準・`transform-origin: top left` へ切り替え、`getBoundingClientRect()`（スケール後寸法）と `clampToolbarToViewport()` でビューポート内に収める。

### 3. 図形ハンドル・クロップ・編集モード・スポイト
- **直線・矢印（`line` / `arrow`）**: 選択時に破線枠は描かず、始点（`start`）・終点（`end`）の 2 点のみ円形ハンドルを表示して独立ドラッグする。
- **矩形・楕円・画像（`rect` / `ellipse` / `image`）**: 四隅（比率維持、`rect` は `Shift` で自由比率）と 4 辺中点（単軸伸縮）の計 8 点ハンドル（`getObjectResizeHandles`）。
- **クロップ（`crop`）**:
  - 領域外を `rgba(0, 0, 0, 0.62)` で暗転マスク（枠線なし）。
  - アクションバー（`.cd-crop-actions`）はツールバーと同じコンテナ配色・角丸・`.cd-action-icon-btn` スタイル・同じ SVG アイコン（コピー・保存・解除）で揃える（勝手に別デザイン・個別色・独自ボタンスタイルを作らない）。
  - `getCropHandleAt` が四隅・4 辺・内側を判定し、`getCropCursor` でホバー／ドラッグ中に `nwse-resize`, `nesw-resize`, `ns-resize`, `ew-resize`, `move`, `crosshair` へ動的切替する。ヒット閾値 `pad` / `edgePad` も `--cd-inv-zoom` 相当で補正済み。
- **貼付画像のフレーミング**: 画像選択中に `Ctrl`（Mac は Cmd）を押している間が**フレーミングモード**（`framingMode`、keydown/keyup/blur で切替）。このモード中はカーソルをクロップ同様の `crosshair` に切替（ハンドル上も含む）。また、このモードのときだけ `drawSelectionBox` がフレーム外の元画像を alpha 0.3 で表示（画面専用・エクスポート非含有）。モード中の操作は 2 通り: (a) 画像（隠れ部分含む `getImageFullRect`）上をドラッグ → 破線矩形（`framingRect`）が離した時点で新しいフレームになる（`applyFramingRect`、元画像範囲内にクランプ、8px 未満は無視）。(b) 8 ハンドルを `Ctrl+ドラッグ` → 拡縮せず辺ごとに表示範囲を増減（`isFraming`）。画像は `sx/sy/sw/sh`（元画像ピクセル）を常に保持し、`drawImageObject` が 9 引数 `drawImage` で描画。元画像の範囲内で戻すことも可。履歴は `MODIFY`。通常ドラッグは従来どおりリサイズ。
- **Web テキスト直接編集（`edit`）**: `document.designMode = 'on'` + `.cd-canvas-wrap` / `canvas` の `pointerEvents = 'none'`。さらに `window` キャプチャフェーズの `click` で `<a>` 遷移を抑止する。終了時は必ず `designMode = 'off'` に戻す。
- **画像の反転**: 通常リサイズで反対側の辺（原点）を超えてドラッグすると `flipX` / `flipY` が切り替わり画像が左右/上下反転（`w`/`h` は常に正）。描画は `drawImageObject` / ゴースト表示とも自身の中心でミラー（`translate(2x+w)`+`scale(-1,1)`）。フレーミング時は反転に応じて辺の対応（w↔e, n↔s）を入れ替える。貼付時に `flipX/flipY:false` を明示（Undo の `Object.assign` 復元で状態が残らないように）。
- **貼付画像の Ctrl+C**: 選択中の `image` は `copyImageObjectAsFramed` で現在のフレーム（`sx/sy/sw/sh`・反転・表示サイズ×`devicePixelRatio`）のまま PNG としてクリップボードへコピー（`tryCopyBlobOrShowModal`）。内部コピー（`copiedDoodleObject`）も保持。他タイプは従来どおり内部 JSON コピー。
- **画像の情報ラベル**: 選択中の `image` の上（画面上端付近は下）に、フレーミング後の元画像 pixel サイズ（`sw×sh`）と元画像比の縦横倍率（`表示サイズ×devicePixelRatio ÷ sw/sh`、貼付直後は 100%）を `drawSelectionBox` 内で描画（スポイトの Hex バッジと同配色）。
- **矢印キー移動**: 選択ツールで選択中に `←↑→↓` で 1px、`Shift` 併用で 10px 移動（`moveObject` 流用、キー毎に `MODIFY` 履歴）。
- **オブジェクト削除**: 選択ツールで選択中に `Delete` / `Backspace`（`REMOVE` 履歴、Undo 可）。`Ctrl+X` は `Ctrl+C` と同じ内部クリップボード（`copiedDoodleObject` + JSON テキスト）へ保存してから削除（カット、`Ctrl+V` で貼付）。
- **スポイト（`eyedropper`）**: 起動時に `hostEl` を隠して `captureVisibleTab` の背景＋既存描画をオフスクリーン合成し、カーソルに十字クロスヘア＋拡大ルーペ＋Hex バッジを描画する（`Shift+クリック` で OS ネイティブ `EyeDropper` API）。

### 4. キャプチャ系（領域・全範囲スクロール・Desk/Win）
- **UI の一時非表示**: `captureVisibleTab` / `getDisplayMedia` のフレーム取得直前に `hostEl.style.visibility = 'hidden'` にし、取得後・`catch`/`finally` で必ず `'visible'` に戻す。
- **Chrome Quota 対策**: 全範囲スクロール撮影（`copyFullPageScrollToClipboard`）は `captureTabWithThrottle()` で最低 `560ms` 間隔を空け、`MAX_CAPTURE_VISIBLE_TAB` エラー時は `650ms` 待ってリトライする。
- **固定要素・スクロール対策**: 2 枚目以降のスクロール中に `position: fixed` / `sticky` 要素を一時非表示にし、完了後 `finally` で元に戻す。`html, body, * { scroll-behavior: auto !important; }` を一時挿入し、`canvas` 上限（`MAX_DIM=30000`, `MAX_AREA=180000000`）内に自動スケールダウンする。
- **Desktop/Window フォールバック**: `navigator.mediaDevices.getDisplayMedia` が非 HTTPS 環境や `Permissions-Policy`（350ms 未満の即時 `NotAllowedError`）でブロックされた場合は `CHROME_DOODLE_OPEN_DESKTOP_CAPTURE` で拡張機能ポップアップ（`desktop_capture.html`）を開く。
- **クリップボードフォールバック**: 自動 `navigator.clipboard.write` が拒否された場合は `showCaptureResultModal` でプレビュー・ワンクリックコピー・キャンバス貼付・PNG 保存ボタンを出す。

## 検証手順・落とし穴
- **構文チェック**: 変更後は必ず各 JS に構文チェックを実行する（この環境では `node` が PATH 外の場合 `C:\Users\kyoya\AppData\Local\mise\shims\node.exe -c` を使用）。
  ```powershell
  & "C:\Users\kyoya\AppData\Local\mise\shims\node.exe" -c ChromeExt/ChromeDoodle/content.js
  & "C:\Users\kyoya\AppData\Local\mise\shims\node.exe" -c ChromeExt/ChromeDoodle/background.js
  & "C:\Users\kyoya\AppData\Local\mise\shims\node.exe" -c ChromeExt/ChromeDoodle/desktop_capture.js
  ```
- **拡張機能の反映**: `background.js` や `content.js` を更新したら、`chrome://extensions/` で「更新（リロード）」を押し、**対象タブもリロード**してから `Alt+Shift+D` 等で動作確認する（旧 content script が残ったタブでは新メッセージや修正が反映されない）。

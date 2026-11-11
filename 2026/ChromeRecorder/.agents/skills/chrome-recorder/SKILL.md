---
name: chrome-recorder
description: >-
  Chrome 拡張 ChromeRecorder (getDisplayMedia + MediaRecorder でデスクトップ画面とシステム音を webm に保存) の構成、
  Playwright による検証手順、検証結果と落とし穴。ChromeRecorder の修正・検証時、ブラウザでの画面・システム音録画の話題で使用する。
---

> 🤖[AI 生成 (Claude)。2026-11-09 のセッションで確認した事実のみ記載]

# ChromeRecorder 開発・検証スキル

## 構成
- [manifest.json](~/projs/samples/2026/ChromeRecorder/manifest.json): Manifest V3。権限なし (未使用だった `downloads` は 2026-11-09 に削除)
- [background.js](~/projs/samples/2026/ChromeRecorder/background.js): アイコンのクリックで `recorder.html` をタブで開く (開いていればそのタブへ移動)
- [recorder.js](~/projs/samples/2026/ChromeRecorder/recorder.js): 録画本体
  - 保存先は初回に `showDirectoryPicker({ id, mode: "readwrite", startIn: "downloads" })` で選んだフォルダ (サブフォルダは作らない。ユーザー指示)。ハンドルを IndexedDB (`chrome-recorder` / `kv` / `dir` → `{ dir }`) に保存。2 回目以降は `queryPermission` → 必要時 `requestPermission` のみでダイアログなし
  - フォルダ選択・許可のダイアログでクリックの効力 (transient activation) が切れて `getDisplayMedia` が `InvalidStateError` になった場合は、もう一度押すようログで案内 (この分岐は未検証)
  - 「変更」ボタンでフォルダを選び直す
  - `getDisplayMedia({ video: { frameRate }, audio: true, systemAudio: "include" })`
  - `MediaRecorder.start(1000)` で 1 秒ごとの `dataavailable` を `writable.write` で逐次書き込み (`writeChain` で順序保証)
  - 音声トラックが 0 なら警告をログ表示。映像トラックの `ended` (共有を停止) でも録画終了
  - ファイル名 `rec_yyyyMMdd_HHmmss.webm` (現地時刻)
  - 🤖 複数画面の同時録画 (2026-11-10 追加。ユーザー要望は「複数メディア」ではなく複数画面): 「+ 画面を追加」で `sources` に追加 → 録画開始時に `buildComposite()` が canvas (原寸横並び、`setInterval` で描画) + AudioContext ミックスを `captureStream(fps)` で 1 ストリーム化。`sources` が空なら従来の `getDisplayMedia`。全ソースのトラックが `ended` になると録画終了。停止後もソースは解放しない (2026-11-10 ユーザー要望「録画停止しても対象画面を閉じない」)。1 画面録画のストリームも停止後 `registerSource()` で一覧へ移して維持
  - 🤖 落とし穴: 映像サイズ確定前に canvas を仮サイズで録画を始めると、webm の解像度が仮サイズ (1280x720) に固定された → `loadedmetadata` を待ってから canvas サイズを決めて解消
  - 🤖 プレビュー (2026-11-10): 画面の追加・削除のたびに `refreshPreview()` が合成 canvas (`comp`) を作り直して `#preview` に表示。録画開始時はこの canvas の映像トラック + 音声ミックスを録画
  - 🤖 落とし穴: canvas の width/height は同じ値を代入してもクリアされる。録画開始時に無条件で代入した版は先頭付近に約 1.1 秒のフレーム欠落 (30 フレーム/5 秒) → 値が変わるときだけ代入して 51-52 フレーム/5 秒、最大間隔 0.16 秒 (欠落と代入の因果は未確認、修正後 2 回とも再発なし)
  - 🤖 UI (2026-11-10 ユーザー指示): fps は「録画開始」の行 (録画中は disabled)。Audio チェックと複数画面枠の説明文 (legend) は「不要」の指示で削除 (`withAudio()` は常に true)。ログ `#log` は最下部、高さ 12em + `overflow-y: auto`、追記時に最下行へスクロール
  - 🤖 録画開始・停止ボタン: U+23FA BLACK CIRCLE FOR RECORD / U+23F9 BLACK SQUARE FOR STOP に U+FE0E (文字表示) を付け、CSS で録画を赤 (`#start:not(:disabled)`)。比較 (Edge 157 / Chrome 157、Windows 11): FE0F 付きの絵文字表示は青い枠の丸・四角、🔴 (LARGE RED CIRCLE) は立体の赤玉、FE0E + CSS と SVG はほぼ同じ見た目。title・aria-label に名前、id は `#start` / `#stop`
  - 🤖 音声の警告 (2026-11-10): Audio ON で音声トラックなしの場合、画面追加時と録画開始時に警告。`getSettings().displaySurface === "window"` なら画面全体での共有を案内。実例: ユーザーのウィンドウ共有 (Windows) で audio=0。「ウィンドウ共有では音声不可」は根拠なし: Chrome 公式 (screen-sharing-controls) は `systemAudio: "exclude"` の例で「audio alongside tabs and windows」を提示、MDN は `windowAudio` (exclude/window/system) を記載。対応バージョン・`windowAudio` の要否は未確認
  - 🤖 停止・分割 (2026-11-11 ユーザー指示「停止は押したらすぐ反応」「録画中の録画ボタンはここまでを保存し新しいファイルで録画」): 1 ファイル = `startSegment()` (MediaRecorder + writable、`r.saved` で保存完了)。`requestStop()` は即ボタン無効化・「保存中…」→ 保存後に共有の後始末。録画中の ⏺ は `splitSegment()`: 新ファイルの録画を先に開始してから旧 recorder を stop。ファイル名重複は `_2`… を付与、0 バイトは削除
  - 🤖 検証 (2026-11-11、Edge + FAKE、close を 2 秒遅延させて保存の遅さを再現): 停止直後 stop=無効・status「保存中…」、保存後に録画可。分割 3 ファイル (複数画面 3840x1080 各 4.3-4.7 秒、合計 13.3 秒 / 実時間約 14 秒)。分割直後 1 秒以内の再押下で 0 バイトのファイルができた → 削除処理を追加 (0 バイトになる理由は未確認)
  - 🤖 自動ローテーション (2026-11-11 ユーザー指示「時間か分で設定、0=自動ローテなし」): `#rotate` (数値、step any) + `#rotateUnit` (分=60 / 時間=3600)。`scheduleRotate()` が setTimeout で `splitSegment()`、分割ごと (手動含む) に再設定、停止で解除。localStorage `chrome-recorder.rotate` / `.rotateUnit` に記憶。検証 (Edge + FAKE、0.05 分): 3 秒ごとに分割、0 では 7 秒録画で 1 ファイル、再読み込み後に 2 / 時間を復元
  - 🤖 音声レベル (2026-11-11、ユーザー要望): `attachMeter()` が各ソースを `AnalyserNode` (行メーター) と master `AnalyserNode` (推移グラフ `#level`、50ms × 600 点) へ接続、destination 非接続。RMS → dBFS (-60..0)。`setInterval` 駆動。検証 (Edge、合成の 440Hz 振幅変化ソースを `registerSource` で注入): メーター幅 63%、グラフが振幅変化に追従、音声なしソースはメーター非表示、音声ソース削除でパネル消滅。実際の共有音声での確認は未実施 (FAKE の共有は無音)
  - 🤖 音声での自動録画 (2026-11-11 ユーザー指示 c5 +「自動開始/終了のチェックボックス」): `#autoStart` で `arm()` (合成ストリームを `startPre()` の MediaRecorder でメモリ録画、10 秒ごとに作り直して古い分を捨てる。立ち上がり候補 `runAbove>0` の間は作り直さない)。`hasSound` (-45 dBFS 以上が 1 秒連続で true、未満が 30 秒連続で false) になると `beginArmed()` がバッファをファイル先頭にして `bindWriter()` で続きを書き込む。`#autoStop`: 録画中の連続無音 (`session.quiet`) が 30 秒で `requestStop(true)` → 自動開始が ON なら再待機。手動停止で自動開始を解除。⏺ を待機中に押すとバッファを引き継いで手動開始
  - 🤖 検証 (Edge、合成 440Hz 音源を `registerSource` で注入、`scratchpad/pw/autock.cjs`): 無音 12 秒待機 → 音 ON で約 3 秒後に録画開始 (先頭 4 秒分のバッファ)、出力 webm は先頭 2.7 秒が無音で音の立ち上がりを含む (silencedetect)。音 OFF 30 秒で保存し待機へ復帰、2 回目も検知、手動停止で自動開始解除。自動終了 OFF では無音 34 秒でも録画継続。実共有音声・しきい値 -45 dBFS の妥当性は未確認
  - 🤖 グラフの表示: 赤の点線 = 音あり判定のしきい値 (-45 dBFS)、棒はしきい値以上が濃色・未満が薄色、しきい値線に重なる赤の太線 = 「音あり」判定中
  - 🤖 ボタン状態の区別 (2026-11-11 ユーザー指摘): `setRecordingUI()` が録画中に開始ボタンを「分割」(class split、枠線) に切替、`#stop` は押せるとき赤塗り・押せないとき枠線のみ。スクリーンショット (Edge) で待機・録画中・保存中の 3 状態を確認
  - 🤖 ファビコン: 2026-11-11 ユーザー指示で「録画中のみ赤丸」に変更 (待機・保存中は既定)。録画中以外は `link[rel=icon] href="data:,"` で既定に戻す (link を消すだけでは前のアイコンが残る可能性があるため。ブラウザ実画面での確認は未実施)。経緯: タブのタイトル・ファビコン (2026-11-11 ユーザー要望「録画中/待機状態も」): `setTabRec(text, wait)` が `document.title` と `link[rel=icon]` (canvas 生成の赤丸=録画 / 赤い輪=待機) を切替。`setStatus` / `requestStop` / `arm` / `disarm` から呼ぶ。Edge で待機→録画→保存中→復帰を確認 (`scratchpad/pw/titleck2.cjs`)
  - 🤖 1 画面録画 (画面を追加せずに ⏺) の最中も、`renderSources()` が録画中のストリームを仮の行 (×は無効、停止後に同名 screenN で登録) として一覧に出す (2026-11-11 ユーザー指摘「録画中に未追加と表示はおかしい」)。Edge で確認 (`scratchpad/pw/one.cjs`)
  - 🤖 1 画面録画 (画面を追加せずに ⏺) の音声レベル (2026-11-11 e9): 録画中のストリームを `tempSrc` (録画中のみ) として `attachMeter` し、行メーター・グラフ・自動終了 (無音 30 秒) を有効化。停止時に `detachMeter` → `registerSource` で一覧へ移す。Edge で確認 (`scratchpad/pw/one2.cjs`: メーター・グラフ表示、無音 30 秒で自動停止・保存・共有維持)。実音声での確認は未実施
  - 🤖 自動分割の単位は分固定 (2026-11-11 ユーザー指示。`#rotateUnit` は廃止、時間指定は分で入力: 例 60 = 1 時間)。localStorage は `chrome-recorder.rotate` のみ
  - 🤖 1 クリックで getDisplayMedia は 1 回しか呼べない (transient activation) ため、ソースはボタンで 1 つずつ追加する設計

## 最低対応バージョン (Web 確認、2026-11-09。根拠 URL は README「参照」)
- 全体: Chromium 系 105 以上 (`systemAudio` が最も新しい)。Firefox / Safari は `showSaveFilePicker` 非対応
- `systemAudio`: Chrome 105 (公式) / `showSaveFilePicker`: Chrome・Edge 86 / Manifest V3: Chrome 88 / `MediaRecorder`: Chrome 49
- OS 別のシステム音: Windows・ChromeOS は画面全体の共有時に可、macOS は Chrome 141 + macOS 14.2 以上 (いずれも第三者情報)

## 検証手順 (Windows 11、Playwright)
スクリプト: [scripts/ext_test.cjs](scripts/ext_test.cjs)

```
node scripts/ext_test.cjs "<ブラウザ exe>" <ChromeRecorder ディレクトリ> <出力 webm> <FAKE|MANUAL|共有元名>
```

- `PW_CORE` に playwright-core のパスを渡すか、通常の `require("playwright-core")` で解決できる場所で実行
- 別タブで 440Hz のテスト音 (WebAudio) を鳴らしてから録画を開始する。`--autoplay-policy=no-user-gesture-required` を付けて起動
- フォルダ選択ダイアログは自動操作できないため、テスト時のみページ側で `showDirectoryPicker` を OPFS ルート (`navigator.storage.getDirectory()`) を返す関数に差し替え、録画後に OPFS から読み出してディスクへ書く (出力名 `<出力>_<回>.webm`)
- `ROUNDS` (既定 2) 回、ページを再読み込みして録画し、2 回目以降にフォルダ選択が呼ばれないこと (IndexedDB の記憶) を確認する。プロファイルは OS 一時フォルダに残るので、初回から確認するときは削除する
- `MANUAL`: 共有ダイアログを人が操作 (「画面全体」+「システム音声も共有」)。90 秒待機後、約 6 秒録画
- `FAKE`: `--use-fake-ui-for-media-stream` で共有ダイアログを省略
- 結果の確認は ffmpeg で行う:
  - `ffprobe -v error -show_entries stream=codec_type,codec_name,width,height,sample_rate,channels -of compact <webm>`
  - `ffmpeg -i <webm> -map 0:a -af volumedetect -f null -` (無音なら約 -91 dB)
  - 440Hz 帯: `-af "highpass=f=400,lowpass=f=480,volumedetect"`

## 検証結果 (2026-11-09、RDP 環境、音声デバイスは「リモート オーディオ」のみ)
- Edge + `MANUAL`: 映像 vp9 1920x1080、音声 opus 48000Hz 1ch、mean_volume -21.9 dB / max -1.8 dB、440Hz 帯 -26.5 dB (テスト音を収録)
- Edge + `FAKE`: 音声トラックはあるが無音 (mean -91.0 dB)。システム音の確認には使えない
- 🤖 実録画 `rec_20261109_162655.webm`(ユーザー操作、ウィンドウ共有): 28:14、136,148,237 バイト(約 4.8 MB/分)、映像 vp9 982x722・16,338 フレーム(平均約 9.6fps、設定 10)、音声 opus 48000Hz 1ch・mean -22.6 dB / max 0.0 dB。途中欠落なし(先頭-末尾まで読み取り可)
- 🤖 保存フォルダ記憶 (Edge + `FAKE`、ROUNDS=2、選択フォルダへ直接保存版): 1 回目でフォルダ選択 1 回 → `rec_20261109_170350.webm` (407789 bytes)、再読み込み後の 2 回目は選択なしで `rec_20261109_170356.webm` (410807 bytes)。OPFS ルートは名前が空のため、画面のフォルダ表示も空になる。実フォルダ (OPFS 以外) での書き込み許可の確認、ブラウザ再起動後の挙動は未検証
- 🤖 複数画面の同時録画 (2026-11-10、Edge + FAKE、テストはページ内で `showDirectoryPicker` を OPFS に差し替え、`#addScreen` ×2 → `#start` 5 秒 → `#stop`): vp9 3840x1080 全 49 フレーム、opus 2ch。同日 `ext_test.cjs` (FAKE、ROUNDS=1) で従来モード 1920x1080 を確認
- `--auto-select-desktop-capture-source` は「画面全体」「Entire screen」「画面 1」「Screen 1」のいずれでも共有が始まらなかった (ログ空のまま待機)

## 落とし穴
- Google Chrome は起動フラグ `--load-extension` で拡張が読み込まれなかった (service worker が現れず)。自動検証は Edge で行う。Chrome への手動読み込み (`chrome://extensions`) は未確認
- 初版はファイル名を `toISOString()` から作っており、UTC かつ秒の 1 桁が欠けていた (`rec_2026110906562`)。現地時刻で組み立てる形に修正済み
- 🤖 MediaRecorder の webm は Duration が入らない(ffprobe で `duration=N/A`、シーク不可の場合あり)。長さは `ffmpeg -i <webm> -map 0:a -f null -` の最終 `time=` で取得。シーク可能にするには `ffmpeg -i in.webm -c copy out.webm` で再多重化(未検証)
- 未確認: 複数ディスプレイ全体の録画 (`gdigrab` の desktop は 3840x1082 だが、拡張の録画は 1920x1080)、音声が 1ch になる理由

## 関連
- ffmpeg 単体では Windows のシステム音を録れない (WASAPI 入力が無い。FFmpeg trac #9408 は機能要望のまま)。dshow の「リモート オーディオ」は録音側で無音だった
- ffmpeg 以外でシステム音だけを録る場合は WASAPI ループバック (Python `PyAudioWPatch`) で取得できた

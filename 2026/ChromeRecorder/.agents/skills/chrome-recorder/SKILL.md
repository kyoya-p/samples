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
- `--auto-select-desktop-capture-source` は「画面全体」「Entire screen」「画面 1」「Screen 1」のいずれでも共有が始まらなかった (ログ空のまま待機)

## 落とし穴
- Google Chrome は起動フラグ `--load-extension` で拡張が読み込まれなかった (service worker が現れず)。自動検証は Edge で行う。Chrome への手動読み込み (`chrome://extensions`) は未確認
- 初版はファイル名を `toISOString()` から作っており、UTC かつ秒の 1 桁が欠けていた (`rec_2026110906562`)。現地時刻で組み立てる形に修正済み
- 🤖 MediaRecorder の webm は Duration が入らない(ffprobe で `duration=N/A`、シーク不可の場合あり)。長さは `ffmpeg -i <webm> -map 0:a -f null -` の最終 `time=` で取得。シーク可能にするには `ffmpeg -i in.webm -c copy out.webm` で再多重化(未検証)
- 未確認: 複数ディスプレイ全体の録画 (`gdigrab` の desktop は 3840x1082 だが、拡張の録画は 1920x1080)、音声が 1ch になる理由

## 関連
- ffmpeg 単体では Windows のシステム音を録れない (WASAPI 入力が無い。FFmpeg trac #9408 は機能要望のまま)。dshow の「リモート オーディオ」は録音側で無音だった
- ffmpeg 以外でシステム音だけを録る場合は WASAPI ループバック (Python `PyAudioWPatch`) で取得できた

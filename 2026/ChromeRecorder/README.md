# Chrome Recorder

> 🤖[AI 生成・編集を含む (Claude)]

# 概要

デスクトップ画面とシステム音を共有し、1 つの webm ファイルに保存する Chrome 拡張機能。

- `getDisplayMedia` (画面 + システム音) を `MediaRecorder` で録画 (映像 VP9 / 音声 Opus)
- 保存フォルダは初回のみ選択 (初期表示はダウンロード) し、以後は記憶したフォルダへダイアログなしで保存
- 1 秒ごとにファイルへ逐次書き込むため、途中でタブを閉じてもそれまでの分は残る
- fps を指定可 (初期値 10)
- 共有ダイアログの「共有を停止」でも録画を終了

# 環境要件

| 項目 | 要件 | 確認状況 |
| --- | --- | --- |
| ブラウザ | Chromium 系 (Google Chrome / Microsoft Edge) 105 以上 (下記 API のうち最も新しい `systemAudio` に合わせる)。Firefox / Safari は `showSaveFilePicker` 非対応のため不可 | Edge で確認済み。Chrome は未確認 |
| 使用 API | `systemAudio` オプション: Chrome 105 (デスクトップ)<br>File System Access API (`showDirectoryPicker`、ハンドルの IndexedDB 保存): 個別の最低バージョンは未確認 (同 API の `showSaveFilePicker` は Chrome / Edge 86)<br>Manifest V3: Chrome 88<br>`MediaRecorder` (WebM VP9/Opus): Chrome 49 | 各バージョンは Web の公開情報 (下記「参照」)。Edge で動作確認済み |
| OS | Windows / ChromeOS: 「画面全体」の共有でシステム音を取得可 (第三者情報)<br>macOS: Chrome 141 以上かつ macOS 14.2 以上で可とする第三者情報あり (公式情報は見つからず)<br>Linux: 情報なし | Windows 11 で確認済み。その他の OS は未確認 |
| 音声出力デバイス | システム音が再生される出力デバイスがあること | RDP の「リモート オーディオ」で確認済み |
| 保存先 | 書き込み可能なローカルフォルダ | — |

# インストール

1. Chrome は `chrome://extensions`、Edge は `edge://extensions` を開く
2. デベロッパー モードを ON
3. 「パッケージ化されていない拡張機能を読み込む」でこのディレクトリ (`ChromeRecorder/`) を選択
4. ツールバーに拡張機能のアイコンが表示されることを確認 (表示されない場合は拡張機能メニューからピン留め)

# 使い方

1. 拡張機能のアイコンをクリック → 録画ページがタブで開く
2. 「録画開始」を押す
   - 初回のみ保存フォルダを選択 → 記憶
   - ブラウザ再起動後などで書き込み許可が切れている場合は、許可の確認が出る
   - 選択・許可の後に共有ダイアログが出ない場合は、ログの案内に従ってもう一度「録画開始」を押す
3. 共有ダイアログで「画面全体」を選び、「システム音声も共有」を ON にして「共有」
4. 「停止」で保存完了。ログに `保存完了: rec_yyyyMMdd_HHmmss.webm (n bytes)` と表示

- 保存フォルダは画面上部に表示。「変更」で選び直す
- 音声トラックが無い場合、ログに警告を表示する

# 動作確認 (2026-11-09、Windows 11、RDP 環境)

- Edge に読み込み、実際の共有ダイアログで録画: 映像 vp9 1920x1080、音声 opus 48000Hz 1ch、mean_volume -21.9 dB (別タブの 440Hz テスト音を収録)
- 共有ダイアログを `--use-fake-ui-for-media-stream` で省略すると、音声トラックはあるが無音 (-91.0 dB)
- Chrome は起動フラグ `--load-extension` では読み込まれない。`chrome://extensions` からの読み込みは未確認
- 未確認: 複数ディスプレイ全体の録画、音声が 1ch になる理由

# 参照 (環境要件の根拠、2026-11-09 確認)

- [Privacy-preserving screen sharing controls (Chrome for Developers)](https://developer.chrome.com/docs/web-platform/screen-sharing-controls): `systemAudio` は Chrome 105 (デスクトップ)
- [How to save a file (web.dev)](https://web.dev/articles/files/save-a-file) / [showSaveFilePicker() (MDN)](https://developer.mozilla.org/docs/Web/API/Window/showSaveFilePicker): Chrome / Edge 86、Firefox / Safari 非対応、secure context が必要
- [Migrate to Manifest V3 (Chrome for Developers)](https://developer.chrome.com/docs/extensions/develop/migrate): Manifest V3 は Chrome 88 から
- [MediaRecorder (Chrome for Developers blog)](https://developer.chrome.com/blog/mediarecorder): Chrome 49 以上
- [Screen recording with system sounds in Chrome (addpipe)](https://blog.addpipe.com/screen-recording-with-system-sounds-in-chrome.md): Windows / ChromeOS は Chrome 74 以上で画面全体の共有時にシステム音 (第三者)
- [getDisplayMedia allows capturing the screen with system sounds on Chrome on macOS (addpipe)](https://blog.addpipe.com/getdisplaymedia-allows-capturing-the-screen-with-system-sounds-on-chrome-on-macos/): macOS は Chrome 141 以上 + macOS 14.2 以上 (第三者)

# ファイル

| ファイル | 内容 |
| --- | --- |
| `manifest.json` | Manifest V3 |
| `background.js` | アイコンのクリックで録画ページを開く |
| `recorder.html` / `recorder.js` | 録画・保存 |

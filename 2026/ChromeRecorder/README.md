# Chrome Recorder

> 🤖[AI 生成・編集を含む (Claude)]

# 概要

デスクトップ画面とシステム音を共有し、1 つの webm ファイルに保存する Chrome 拡張機能。

- `getDisplayMedia` (画面 + システム音) を `MediaRecorder` で録画 (映像 VP9 / 音声 Opus)
- 保存フォルダは初回のみ選択 (初期表示はダウンロード) し、以後は記憶したフォルダへダイアログなしで保存
- 1 秒ごとにファイルへ逐次書き込むため、途中でタブを閉じてもそれまでの分は残る
- fps を指定可 (初期値 10)
- 共有ダイアログの「共有を停止」でも録画を終了
- 複数画面の同時録画: 追加した画面を原寸で横並びにして 1 ファイルに録画 (音声はミックス)

# 環境要件


| 項目             | 要件                                                                                                                                                                                                                                                                                        | 確認状況                                                          |
| ---------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| ブラウザ         | Chromium 系 (Google Chrome / Microsoft Edge) 105 以上 (下記 API のうち最も新しい`systemAudio` に合わせる)。Firefox / Safari は `showSaveFilePicker` 非対応のため不可                                                                                                                        | Edge で確認済み。Chrome は未確認                                  |
| 使用 API         | `systemAudio` オプション: Chrome 105 (デスクトップ)<br>File System Access API (`showDirectoryPicker`、ハンドルの IndexedDB 保存): 個別の最低バージョンは未確認 (同 API の `showSaveFilePicker` は Chrome / Edge 86)<br>Manifest V3: Chrome 88<br>`MediaRecorder` (WebM VP9/Opus): Chrome 49 | 各バージョンは Web の公開情報 (下記「参照」)。Edge で動作確認済み |
| OS               | Windows / ChromeOS: 「画面全体」の共有でシステム音を取得可 (第三者情報)<br>macOS: Chrome 141 以上かつ macOS 14.2 以上で可とする第三者情報あり (公式情報は見つからず)<br>Linux: 情報なし                                                                                                     | Windows 11 で確認済み。その他の OS は未確認                       |
| 音声出力デバイス | システム音が再生される出力デバイスがあること                                                                                                                                                                                                                                                | RDP の「リモート オーディオ」で確認済み                           |
| 保存先           | 書き込み可能なローカルフォルダ                                                                                                                                                                                                                                                              | —                                                                |

# インストール

1. Chrome は `chrome://extensions`、Edge は `edge://extensions` を開く
2. デベロッパー モードを ON
3. 「パッケージ化されていない拡張機能を読み込む」でこのディレクトリ (`ChromeRecorder/`) を選択
4. ツールバーに拡張機能のアイコンが表示されることを確認 (表示されない場合は拡張機能メニューからピン留め)

# 使い方

1. 拡張機能のアイコンをクリック → 録画ページがタブで開く
2. ⏺ (録画開始) を押す
   - 初回のみ保存フォルダを選択 → 記憶
   - ブラウザ再起動後などで書き込み許可が切れている場合は、許可の確認が出る
   - 選択・許可の後に共有ダイアログが出ない場合は、ログの案内に従ってもう一度 ⏺ (録画開始) を押す
3. 共有ダイアログで「画面全体」を選び、「システム音声も共有」を ON にして「共有」
4. ⏹ (停止) で保存完了。ログに `保存完了: rec_yyyyMMdd_HHmmss.webm (n bytes)` と表示

- 🤖 複数画面の同時録画: ⏺ (録画開始)の前に「+ 画面を追加」を画面の数だけ押し、共有ダイアログで画面 1、画面 2… を 1 つずつ選ぶ (1 クリックにつき 1 回しか共有ダイアログを出せないため)。追加した時点から下のプレビューに横並びの状態を表示する。追加した画面があれば、録画開始でそれらを横並びにして 1 ファイルに録画する。追加がなければ従来どおり 1 画面を録画する。録画を停止しても画面の共有は続き、一覧とプレビューに残る (そのまま次の録画に使える。不要な画面は「削除」で共有終了)。追加なしの 1 画面録画も、停止後はその画面を一覧に移して共有を維持する
- 🤖 録画中に ⏺ (録画) を押すと、ここまでを保存して新しいファイルで録画を続ける (同じ秒なら `_2` 付き。データ 0 のファイルは削除)
- 🤖 音声レベル表示: 追加した画面の行に横棒メーター (音声あり画面のみ)、プレビュー下に全画面合計の直近 30 秒の推移グラフ (横線は -30 / -10 dBFS)。録画待ち・録画中とも表示。再生・録画には影響しない
- 🤖 音声での自動録画 (チェックボックス 2 つ): 「自動開始」= 待機して音が出たら録画開始 (開始前の最大約 10 秒分をメモリにためておき、ファイルの先頭に付ける)、「自動終了」(初期値 ON) = 無音が 30 秒続いたら保存して停止 (自動開始が ON なら待機へ戻る)。画面を追加してから ON にする (待機中は画面の追加・削除不可)。手動で停止すると自動開始は解除。判定: 合計音量 -45 dBFS 以上が 1 秒続くと「音あり」。グラフの赤い点線 = このしきい値、しきい値の線に重なる赤い太線 = 「音あり」判定中
- 🤖 ボタンの状態: 待機中は「⏺ 録画」(黒) と停止は枠線のみ (押せない)。録画中は「⏺ 分割」(枠線) に変わり、「⏹ 停止」が赤の塗りつぶし (押せる)。保存中はどちらも薄い表示
- 🤖 タブの表示: 録画中はタイトルが「REC mm:ss | Desktop Recorder」、保存中は「保存中… | …」、自動開始の待機中は「待機中 | …」 。ファビコンは録画中のみ赤丸 (待機中・保存中・通常は既定のまま)
- 🤖 自動分割: fps の右で間隔を分で指定 (0 = 自動分割なし、小数可)。録画中は変更不可。手動で分割するとそこから数え直す。値はブラウザに記憶
- 🤖 ⏹ (停止) は押した時点で無効化し「保存中…」を表示。保存完了までは ⏺ も押せない
- 🤖 ⏺ (録画開始)の左に fps。録画中は変更不可。音声は常に要求する
- 🤖 ログは画面最下部 (高さ固定・スクロール、最新行へ自動スクロール)
- 保存フォルダは画面上部に表示。「変更」で選び直す
- 音声トラックが無い場合、ログに警告を表示する

# 参照 (環境要件の根拠、2026-11-09 確認)

- [Privacy-preserving screen sharing controls (Chrome for Developers)](https://developer.chrome.com/docs/web-platform/screen-sharing-controls): `systemAudio` は Chrome 105 (デスクトップ)
- [How to save a file (web.dev)](https://web.dev/articles/files/save-a-file) / [showSaveFilePicker() (MDN)](https://developer.mozilla.org/docs/Web/API/Window/showSaveFilePicker): Chrome / Edge 86、Firefox / Safari 非対応、secure context が必要
- [Migrate to Manifest V3 (Chrome for Developers)](https://developer.chrome.com/docs/extensions/develop/migrate): Manifest V3 は Chrome 88 から
- [MediaRecorder (Chrome for Developers blog)](https://developer.chrome.com/blog/mediarecorder): Chrome 49 以上
- [Screen recording with system sounds in Chrome (addpipe)](https://blog.addpipe.com/screen-recording-with-system-sounds-in-chrome.md): Windows / ChromeOS は Chrome 74 以上で画面全体の共有時にシステム音 (第三者)
- [getDisplayMedia allows capturing the screen with system sounds on Chrome on macOS (addpipe)](https://blog.addpipe.com/getdisplaymedia-allows-capturing-the-screen-with-system-sounds-on-chrome-on-macos/): macOS は Chrome 141 以上 + macOS 14.2 以上 (第三者)

# ファイル


| ファイル                        | 内容                                 |
| ------------------------------- | ------------------------------------ |
| `manifest.json`                 | Manifest V3                          |
| `background.js`                 | アイコンのクリックで録画ページを開く |
| `recorder.html` / `recorder.js` | 録画・保存                           |

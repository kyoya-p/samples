---
name: kabuka
description: >
  株価ダッシュボード Kabuka（projs/samples/2026/Kabuka）の構成・運用知識。
  トリガー: "Kabuka", "株価", "銘柄", "sharp_dashboard", "fetch_stock", "tickers.txt", "IFFT", "周期スペクトル" など。
---
<!-- 🤖 AI生成: Kabuka の構成・運用知識 (2026-11-15 時点) -->

# 対象
`C:\Users\kyoya\projs\samples\2026\Kabuka\`（Python + 単一 HTML 生成。チャートは Lightweight Charts 4.1.3 / Chart.js、CDN 読込）

# ファイル
- `fetch_stock.py`: yfinance で全期間日足を採取し `<コード>_daily_all_history.csv` に保存。引数なしなら `tickers.txt` を読む。`.T` は自動付与
- `tickers.txt`: 1行1銘柄 `コード  # 名称`。ダッシュボードの銘柄名にも使う
- `build_dashboard.py`: `*_daily_all_history.csv` を全て読み込み `sharp_dashboard.html` を生成（HTML は f-string、JS の `{}` は `{{ }}` でエスケープ）。シャープ(6753)は `sharp_6753_daily_all_history.csv`、名称は既定で「シャープ」
- `server.py`: 配信＋銘柄追加 API。`POST /api/add?code=XXXX` → 採取 → `tickers.txt` 追記 → `build_dashboard.py` 再実行。ポートは引数（既定 8765）
- 旧スクリプト: `plot_*.py`, `fourier_*.py`（matplotlib 系、`sharp_6753_daily_1y.csv` 等を使用）、`sharp_data.json`（旧データ形式、現在は未使用）

# 実行
- Windows に Python 無し。`mise exec -- python <script>`（pandas・yfinance は pip 導入済み）
- ブラウザ確認: `file:` は T3 の preview で開けない。`mise exec -- python server.py 8766` を起動して `http://localhost:8766/sharp_dashboard.html`。終了後は `Get-CimInstance Win32_Process` で CommandLine に `server.py 8766` を含むものだけ停止
- 8765 番は PID 0 の待受が残り停止不可（2026-11-15 時点）。確認には別ポートを使う

# ダッシュボード仕様
- 銘柄切替: プルダウン＝`?code=XXXX` で再読込。コード入力欄＋「追加・採取」は `server.py` 経由のみ有効（静的配信では採取不可）
- チャート: カレンダー日等間隔（休日は `isHoliday` の補間行）。ローソク足/ライン切替、SMA25/75/200、出来高ヒストグラム
- 軸ラベル（最終値タグ）は全系列で非表示。代わりにチャート右上へ HTML 凡例（終値・SMA）を表示
- IFFT 逆算波: 表示区間の FFT 上位 N 成分を逆変換して重畳。初期はオフ。オンのとき周期スペクトル欄 (`#freqCard`) を表示
- 第1主周期波のチェックは UI 撤去（`#toggleTop1` は hidden・常にオフ）。見出し「Time Domain…」も撤去済み

# 注意・既知事項
- yfinance は調整後価格。取得値は実取引価格と異なる（例: トヨタ 1999 年が約 391 円）。外部情報との照合は未実施
- Lightweight Charts v4 では series option の `scaleMargins` が効かない → `tvChart.priceScale('').applyOptions()` で指定
- 系列の `title` を付けると軸にタグが出る（`lastValueVisible:false` でも）。タグを出したくなければ title を付けない
- 凡例の色: ライン表示時の終値は `#38bdf8`（ローソク足は赤）。要整合（ユーザー指摘「色が違うのでは」、2026-11-15）
- 未対応（2026-11-15 指示）: 凡例行のクリックで SMA/IFFT のオン・オフ、IFFT 値を凡例へ移動（軸の「IFFT逆算波(N成分)」タグ撤去）、ヘッダーの SMA/IFFT チェック撤去

# 銘柄コード（Web 照合済み: JPX 時価総額順位表 2026-03、6753 のみ未確認）
6753 シャープ / 6758 ソニーグループ / 7203 トヨタ自動車 / 7974 任天堂 / 9984 ソフトバンクグループ

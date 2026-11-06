---
name: candlesim
description: >
  線香の煙シミュレータ CandleSim（projs/samples/2026/CandleSim）の構成・運用知識。
  トリガー: "CandleSim", "candle_incense", "線香", "sonnet", "flash_midium", "煙シミュ" など。
---

# 対象
`C:\Users\kyoya\projs\samples\2026\CandleSim\`（単一 HTML・Canvas 2D・外部依存なし）
- `candle_incense_sonnet.html`: 主実装（筋 NC=11）
- `candle_incense_flash_midium.html`: 参照実装（筋 NC=9、霞スプライトなし。GitHub Pages の index.html の元）
- `candle_incense_argon.html`: 別系統の参照

# モデル
- 煙は毎フレーム放出する「リボン行」の列。各行に複数の筋。火種を熱源とする浮力プルーム、Grashof 数による層流→乱流遷移（スライダー: 遷移高さ）
- カオス: curl ノイズ＋ローレンツ（`LZ`、z を加速度として積分 `r.lv`）。上下方向にも作用
- 突風は局所ガウス、マウスは渦対（wake）。速度依存の拡散・消散（`FAST_TH/FAST_FADE/FAST_SPREAD`）
- 固定 60Hz アキュムレータ（`frame` → `tick` → `render`）
- S 字蛇行（`r.dx`）は排除済み。燃焼速度は固定 `S.len -= 0.01`（スライダー削除済み）

# 状態・操作
- 初期状態: 風 0・室内対流 0・線香は最大長（先行 360 ステップ後に `S.len = S.maxLen`）
- 置き方ボタン: `S.lay` / `S.elev` / `S.maxLen`。立てる＝垂直（`ELEV = π/2`）・長さ `MAX_LEN`・香立て球あり、寝かせる＝水平（0.04rad）・長さ 120・球なし。切替時に「新しく供える」と同じリセット
- 発煙停止（`S.len ≤ 14`、flash は 12）で火種・灰を消し、頭を黒く描画（消火）

# 検証手順
- puppeteer-core ＋ Chrome（headless）で撮影。`setViewport` を使う（`--window-size` は初期サイズ差で見かけの欠落が出る）
- JS 構文エラーは `pageerror` を拾うと早い。描画が真っ暗なら構文エラーを疑う
- 実写比較: YouTube hrldJ6vCpv4（t=249s 付近）。実物は層流の細い柱→小スケール巻き込み渦（螺旋）→薄い襞状煙膜

# デプロイ（GitHub Pages）
- `kyoya-p/samples` の `gh-pages` / root。`index.html` は flash_midium のコピーで、master 更新時は手動で再反映（自動同期なし）
- gitlink（mode 160000）が `.gitmodules` なしで存在するとビルド失敗。`2026/dockercompose-gui` は master / gh-pages とも削除済み
- push は人間の承認要（AGENTS.md）

---
name: candlesim
description: >
  線香の煙シミュレータ CandleSim（projs/samples/2026/CandleSim）の構成・運用知識。
  トリガー: "CandleSim", "candle_incense", "線香", "sonnet", "incense_claude", "flash_midium", "煙シミュ" など。
---

# 対象
`C:\Users\kyoya\projs\samples\2026\CandleSim\`（単一 HTML・Canvas 2D・外部依存なし）
- `incense_claude.html`: 主実装（筋 NC=11。旧 `candle_incense_sonnet.html`、現存せず）。Pointer Events 対応（マウス・タッチ）
- `candle_incense_flash_midium.html`: 参照実装（筋 NC=9、霞スプライトなし、rAF 駆動、マウス専用）。gh-pages の旧 index.html の元
- `candle_incense_argon.html`: 別系統の参照（class 構成・横風/燃焼速スライダー・regime バッジ・斜め線香 0.56rad・DPR 未対応）
- `mise.toml`: `pages-deploy` タスク

# モデル
- 煙は毎フレーム放出する「リボン行」の列。各行に複数の筋。火種を熱源とする浮力プルーム、Grashof 数による層流→乱流遷移（スライダー: 遷移高さ）
- カオス: curl ノイズ＋ローレンツ（`LZ`、z を加速度として積分 `r.lv`）。上下方向にも作用
- 突風は局所ガウス、ポインタは渦対（wake）。速度依存の拡散・消散（`FAST_TH/FAST_FADE/FAST_SPREAD`）
- 固定 60Hz アキュムレータ（`frame` → `tick` → `render`）。flash は rAF 毎に 1 ステップでリフレッシュレート依存
- S 字蛇行（`r.dx`）は排除済み。燃焼速度は固定 `S.len -= 0.01`（スライダー削除済み）

# 状態・操作
- 初期状態: 風 0・室内対流 0・線香は最大長（先行 360 ステップ後に `S.len = S.maxLen`）
- 置き方ボタン: `S.lay` / `S.elev` / `S.maxLen`。立てる＝垂直（`ELEV = π/2`）・長さ `MAX_LEN`（claude 170 / flash 168）・香立て球あり、寝かせる＝水平（0.04rad）・長さ 120・球なし。切替時に「新しく供える」と同じリセット
- 発煙停止（`S.len ≤ 14`、flash は 12）で火種・灰を消し、頭を黒く描画（消火）
- 入力（incense_claude）: Pointer Events でマウス・タッチ・ペン共通。マウスはホバー、タッチ・ペンは接触中のみ渦生成（`ptrs` に pointerId ごと、マルチタッチ可）。タップ＝クリック（移動 <10px、タッチは <350ms）。香皿・線香のドラッグ判定はマウス 14px／タッチ 26px
- スマホ対応: canvas `touch-action: none`、操作バーは safe-area 考慮。卓面 `min(H*0.82, BAR_TOP-16)` で折り返した操作バーに香皿が隠れない（`BAR_TOP` は `resize()` で取得）。ドラッグ右端余白は幅 <760px で 190px（通常 260px）

# 既知の課題（外部知見との齟齬）
- プルーム中心速度の減衰指数: 点熱源 MTT 理論は z^-1/3。実装は (1+h/140)^-0.65（flash/claude）、argon は -0.62。温度 ΔT ∝ h^-5/3 は理論どおり
- 燃焼速度 0.01px/f ≒ 約 4.5cm/分（1cm=8px、60fps）。実物の約 0.5〜0.7cm/分の 7〜9 倍
- claude の霞スプライト（`r.turb < 2` で常にスキップ）はデッドコード
- 突風頻度 0 の表記が初期（"0回/分"）と入力後（"なし"）で不一致（flash/claude）
- flash はリセット時に `S.phi`・`phiC`・`emit.lx` を初期化しない（claude は初期化）
- argon: DPR 未対応・rAF 依存・prewarm が regime を無視

# 検証手順
- puppeteer-core ＋ Chrome（headless、`C:\Program Files\Google\Chrome\Application\chrome.exe`）で撮影。`setViewport` を使う（`--window-size` は初期サイズ差で見かけの欠落が出る）。puppeteer-core は未常駐のため scratchpad で `npm i puppeteer-core`
- タッチ検証: `setViewport({ isMobile: true, hasTouch: true })` ＋ `page.touchscreen`。`S.wakes.length` / `S.hxf` で渦生成・ドラッグを確認。スマホ幅（390×844）では操作バーが約 170px になる
- JS 構文エラーは `pageerror` を拾うと早い。描画が真っ暗なら構文エラーを疑う
- 実写比較: YouTube hrldJ6vCpv4（t=249s 付近）。実物は層流の細い柱→小スケール巻き込み渦（螺旋）→薄い襞状煙膜

# デプロイ（GitHub Pages）
- `kyoya-p/samples` の `gh-pages` / root。旧来は gh-pages 独自コミットで root `index.html`（flash_midium のコピー）を保持。master には root `index.html` なし → `pages-deploy` 実行で消える
- `mise run pages-deploy`（`CandleSim/mise.toml`）: `origin/master` を gh-pages へ force push のみ。対象は最後に fetch した時点の origin/master
- ルート `mise.toml` の `gh-pages` タスクはローカル HEAD を force push（同系統）
- mise タスクを pwsh で書く場合: `bash` は WSL スタブに解決されるため不可。関数名は大小文字非区別で `git` を隠すので `git.exe` を呼ぶ
- gitlink（mode 160000）が `.gitmodules` なしで存在するとビルド失敗。`2026/dockercompose-gui` は master / gh-pages とも削除済み
- push は人間の承認要（AGENTS.md）

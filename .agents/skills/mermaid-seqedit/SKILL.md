---
name: mermaid-seqedit
description: Mermaid sequenceDiagram 単一HTMLエディタ (seqedit.html) の設計・SVGレイアウト仕様と、主要8シーケンス図レンダラ比較表 (seq_renderers_comparison.html) のリファレンス。シーケンス図エディタの改修やレンダラ比較時に参照。
generated: AI
update: 2026-11-20
---

<!-- 🤖 AI 生成: 2026-11-20 作成。seqedit.html の改修内容およびシーケンス図レンダラ比較表に基づく -->

# mermaid-seqedit — Mermaid シーケンス図エディタ & レンダラ比較

## 対象ファイル

- エディタ本体: `C:\Users\kyoya\projs\samples\2026\mdedit\mermaid_seq\seqedit.html`
- 8レンダラ比較表: `C:\Users\kyoya\projs\samples\2026\mdedit\mermaid_seq\seq_renderers_comparison.html`

## `seqedit.html` の主要仕様・レイアウト規約

1. **対応矢印 (`ARROWS` / `MSG` 正規表現)**
   - 前向き・双方向10種: `->>`, `-->>`, `->`, `-->`, `-x`, `--x`, `-)`, `--)`, `<<->>`, `<<-->>`
   - 逆向き・左折返し6種: `<-`, `<--`, `<<-`, `<<--`, `(-`, `(--`（自己呼び出しでは左側ループ描画、参加者間では逆向き矢印として描画）
   - 双方向矢印（`<<->>`, `<<-->>`）は `marker-start="url(#mF)"` と `orient="auto-start-reverse"` で両端塗り矢じりを描画。

2. **自己呼び出し (`a === b`) の描画**
   - 右折返し（通常）: `M${x1} ${y}H${x1+36}V${y+10}H${x1+2}`、ラベル中心 `x1 + 18`（`data-self="1"`）。
   - 左折返し（`<-`, `<--`, `<<-`, `<<--`, `(-`, `(--`）: `M${x1} ${y}H${x1-36}V${y+10}H${x1-2}`、ラベル中心 `x1 - 18`（`data-self="-1"`）。`#bFlip` / 右クリックメニューで左右反転可能。

3. **ラベル配置・クランプ・自動折り返し (`wrapTxt` / `clampCX`)**
   - **ブロック枠からはみ出す**: `loop` / `alt` 等の複合フラグメント枠の横幅計算 (`touch(a, b)`) にはメッセージラベル幅 (`tw(txt)`) を含めず、矢印の端点座標のみを渡す。
   - **全体図の範囲内に収める**:
     - 全体図の左端 `dL = x[0] - bw[0]/2` 〜 右端 `dR = x[n-1] + bw[n-1]/2`（最大幅 `maxW = dR - dL`）を基準とする。
     - `maxW` を超える長いメッセージラベルは `wrapTxt`（`ctx.measureText` ベース）で自動的に複数行へ折り返す。
     - 矢印中心を基本としつつ、`clampCX` で `[dL + w/2, dR - w/2]` に中心座標をクランプして全体図の左右端からはみ出さないようにする。

4. **矢印間の縦間隔**
   - 通常メッセージ: `y = cursor + Ln * LHT + 4`、次要素への `cursor = y + 10`（ラベルベースラインは `y - 5`）。
   - 自己呼び出し: `y = cursor + Ln * LHT + 4`、次要素への `cursor = y + 16`。

5. **グループ囲み (`box [Color] [Title] ... end`) と途中生成・消滅 (`create` / `destroy`)**
   - `box`: `isCssCol` + `parseBox` で `mermaid.js` (`sequenceDb.ts` の `parseBoxData`) と同等に `CSS.supports('color', c)`（ただし本家 Mermaid / Obsidian 互換のため `#hex` は除外）で先頭トークンを判定（CSS 標準 148 色名、`transparent`、`rgb()` / `rgba()` / `hsl()` / `hsla()`、CSS Color 4 の `rgb(r g b / a)` 形式に対応）。色指定なら背景色として `pboxBg` 最背面レイヤに描画し、残りをタイトルとして解釈。
   - `create participant|actor ...`: 直後にその参加者が関わるメッセージがあれば生成メッセージの Y 位置にボックスを配置し、矢印をボックス側面に接続。
   - `destroy <id>`: 本家 Mermaid / Obsidian 準拠で、直後にその参加者が関わるメッセージがある場合はそのメッセージの直後（`dy = Math.max(cursor + 6, my + 18)`）に `✕` マークを描画してライフラインを停止し（単独行時もフォールバック描画）、開いている活性バーを閉じて末尾の参加者ボックスを非表示化。

6. **背景ハイライト (`rect`) と `box` の色・透過仕様・描画順**
   - `rect` は `pboxBg` の前面・ライフライン/メッセージの背面に描画される。
   - `bg`（`rect` および `alt`/`loop` 等の複合フラグメントブロック）はネスト深度昇順・面積降順（`a.depth - b.depth || b.area - a.area`）でソートして描画し、外側・大きいブロックほど背面（下層）、内側・小さいブロックほど前面（上層）に重なるようにする。
   - CSS 標準 148 色名、`transparent`、`rgb()` / `rgba()` / `hsl()` / `hsla()`、CSS Color 4 `/` 区切り α 指定に対応（`#hex` は廃止・非対応）。
   - `rgba()` / `hsla()` / `/` 付き α 指定時は `fill-opacity: 1` で指定 α をそのまま適用、`rgb()` / `hsl()` / CSS 色名など不透明色を指定した場合は自動で半透明（`rect`: `0.35`, `box`: `0.22`）にする。

7. **参加者 (`participant` / `actor`) のカスケード削除**
   - `deleteLine(n)` で参加者宣言行を削除した際も `deletePart(i)` に委譲する。
   - `deletePart(i)` は該当参加者の宣言行（および単独メンバーだった `box ... end`）に加え、接続する全メッセージ矢印 (`it.k === 'msg'`)、活性制御 (`it.k === 'act'`)、`destroy` (`it.k === 'destroy'`)、および `Note` の対象リスト（単独対象なら Note 行ごと）を一括削除する。

8. **デフォルトソース (`SAMPLE`) とサンプル読込ボタン (`#bSample`)**
   - `SAMPLE` 定数にレンダラの全対応表現（`title`、`autonumber`、`box ... end` (`rgba`)、`actor` / `participant` と `<br/>` 改行、`U->W` / `W-->U` / `U->>U` / `U<-U` を含む矢印各種、`create` / `destroy`、`+`/`-` および `activate`/`deactivate` によるネスト活性区間、長文自動折り返し、Note 3種 `left of` / `right of` / `over`、複合フラグメント・背景ハイライト全7種 `rect` (`rgba` + ネスト `rgb`) / `alt`+`else` / `loop` / `opt` / `break` / `par`+`and` / `critical`+`option`、`%%` コメント）を網羅格納。
   - ヘッダーの「サンプル」ボタン (`#bSample`) でいつでも `SAMPLE` を再読込可能。

9. **`box` / `rect` カラーピッカー (`#colPop`・右クリッククイックスウォッチ)**
   - **起動方法**:
     - `rect` 枠のダブルクリック、タイトルなし `box` のダブルクリック、または `box` インライン編集横の `🎨 色` ボタン (`#editCol`)。
     - `box` / `rect` 右クリックメニューの「🎨 カラーピッカーで色変更...」および「色 (クイック選択)」スウォッチ行。
   - **機能**: Figma / Chrome DevTools 風のカスタム HSV カラーピッカーを採用。2D 彩度・明度スペクトラム面 (`#cpSV`、無彩色時も色相/彩度を保持)、Hue / Alpha カスタムトラック (`#cpH` / `#cpA`)、変更前/現在の比較スウォッチ (`#cpPv`・クリックで元の色にリセット)、`EyeDropper` スポイト (`#cpEye`・対応ブラウザのみ)、RGBA ⇄ HSLA 出力形式切替 (`#cpFmt`) 付きチャンネル数値入力 (`#cpN0..3`)・任意 CSS 色文字列入力 (`#cpTxt`)、および16色パレットスウォッチ (8列×2段) を双方向同期。ヘッダードラッグ移動・矢印キー微調整に対応し、変更中は Undo 履歴を汚さず SVG のみライブプレビュー、`適用`/外側クリック/`Enter` で 1 ステップとして確定、`キャンセル`/`Esc` で元に戻す。`box` 変更時はタイトル (`rawTitle`) を保持して先頭色トークンのみ差し替える。

## シーケンス図レンダラ比較 (`seq_renderers_comparison.html`)

比較対象8レンダラ: `mermaid.js (公式)`, `seqedit.html`, `PlantUML`, `SequenceDiagram.org`, `WebSequenceDiagrams`, `D2 (sequence)`, `Swimlanes.io`, `js-sequence-diagrams`

- **セクション1（メッセージ・矢印表現 全18項目）**:
  - 実線/破線 × 矢じり無し/塗り/開/半矢じり(上下)/✕終端/●終端/双方向、自己呼び出し(右折返し `A->A` / 左逆折返し `A<-A`)、外部ゲート (`[->A`)。
- **セクション2（矢印ラベル表現 全7項目）**:
  - 基本付与・省略、複数行・改行、文字装飾、文字色・矢印色変更、リンク/アイコン/数式、配置揃え、番号書式 (`Autonumber` 連携)。
- **セクション3（参加者・活性区間・複合フラグメント・注釈 全11項目）**:
  - 矩形/人型/別名、特殊形状 (DB/Queue)、Box囲み、途中生成・消滅 (`create`/`destroy`)、活性バー、時間遅延、複合フラグメント (`alt`/`loop` 等)、背景ハイライト (`rect`)、ノート、区切り線/参照 (`==` / `ref`)、自動採番。
- **セクション4（背景ハイライト・Box の色指定 & 透過仕様サンプル）**:
  - 対応5レンダラ（`mermaid.js`, `seqedit.html`, `PlantUML`, `SequenceDiagram.org`, `D2`）の具体的な色指定・透過サンプルコードを収録。

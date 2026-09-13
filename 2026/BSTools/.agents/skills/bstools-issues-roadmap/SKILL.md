---
name: bstools-issues-roadmap
description: >-
  BSTools (Battle Spirits Tools) の検出済み課題・未実装機能・アーキテクチャ上の技術的負債および将来の検討ロードマップ。
  ルール再現、通信・サーバー、UI/UX、データローダーの改善やリファクタリング時に参照する。
---

# BSTools 検出課題・技術的負債ロードマップ

本書は、BSTools (GameServer, Playmats, model) のコード精査および動作検証によって検出された課題・未実装機能・アーキテクチャ上の改善点を体系的にまとめた将来検討資料である。

---

## 1. ルール再現性・バトルシミュレーションの課題

### 1-1. カード効果の未実装（完全バニラ挙動）
- **現状**: マジック・スピリット・ネクサスの固有効果（召喚時、アタック時、常時効果等）の解決ロジックが存在せず、コスト支払とトラッシュ移動のみ行う。
- **影響**: カードの効果によるゲーム展開や除去・ドロー等のシミュレーションが不可。
- **参照箇所**: [`model/src/Rules.kt` L794](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/Rules.kt#L794)
- **検討案**: カードNoごとのEffectResolverまたはスクリプト実行基盤の導入。

### 1-2. フラッシュタイミング・バーストのスキップ
- **現状**: `Step.FLASH_TIMING`, `Step.ATTACK_START`, `Step.BATTLE_RESOLUTION` 等の定義はあるが、`advanceStep` および `applyAction` ではアタック宣言から直ちに `BLOCK_DECLARATION` に進み、ブロック解決後は即座に `ATTACK_DECLARATION` に戻る。
- **影響**: アタック中・ブロック前後のフラッシュマジック・アクセルの使用やバースト宣言が不可。
- **参照箇所**: [`model/src/Model.kt` L77](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/Model.kt#L77), [`model/src/Rules.kt` L805](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/Rules.kt#L805), [`model/src/Rules.kt` L628-L643](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/Rules.kt#L628-L643)
- **検討案**: フラッシュ優先権のパス・アクション選択サイクルの正式実装。

### 1-3. ブレイヴ・合体・アルティメットの未対応
- **現状**: カテゴリ定義 (`CardCategory.BRAVE`, `ULTIMATE`) のみ存在し、合体スピリットのシンボル加算・コスト計算・合体アタック等のルールが未実装。
- **影響**: ブレイヴやアルティメットを含むデッキのシミュレーションが不可。
- **参照箇所**: [`model/src/Model.kt` L42-L43](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/Model.kt#L42-L43), [`model/src/Rules.kt` L508-L513](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/Rules.kt#L508-L513)

---

## 2. 通信・バックエンドアーキテクチャの課題

### 2-1. Windows Winsock `select` の fd_set 上限 (64) とソケット枯渇
- **現状**: `readFds.fd_count < 64` でガードされており、常時接続の SSE クライアントや切断残渣が蓄積すると、64個目以降の接続が一切処理されず無応答・ハング状態となる。
- **影響**: 複数クライアントやテストの連続実行でサーバーがタイムアウト・ハングする。
- **参照箇所**: [`Playmats/src/Main.kt` L782-L786](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/src/Main.kt#L782-L786)
- **検討案**: `poll` / `WSAPoll` への移行、または不要ソケットの積極的クローズとタイムアウト回収。

### 2-2. HTTP リクエストの分割受信・バッファリング不足
- **現状**: `recv(cs, buffer, 8191, 0)` を1度呼ぶのみで、TCP 分割受信や `Content-Length` 分の読み切り処理が欠落。
- **影響**: 大きなリクエスト本文が途中で切断され、不正な JSON として破棄される。
- **参照箇所**: [`Playmats/src/Main.kt` L826-L830](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/src/Main.kt#L826-L830), [`GameServer/src/Main.kt` L29-L45](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/GameServer/src/Main.kt#L29-L45)
- **検討案**: GameServer に実装されている `readHttpRequest` (Content-Length 読み切り) を Playmats にも導入。

### 2-3. 文字列切り出しによるアドホックな JSON パース
- **現状**: リクエストパラメータを `substringAfter("\"index\"")` などの文字列検索で抽出している。
- **影響**: JSON のキー順序変更、改行、エスケープの混入によりパースエラーが発生。
- **参照箇所**: [`Playmats/src/Main.kt` L951-L956](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/src/Main.kt#L951-L956), [`Playmats/src/Main.kt` L877-L914](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/src/Main.kt#L877-L914)
- **検討案**: `kotlinx.serialization` による型安全な DTO デシリアライズに統一。

### 2-4. SSE 配信時のノンブロッキング即時切断
- **現状**: `broadcastState()` 内の `send()` が戻り値 `<= 0` (一時的バッファフル `WSAEWOULDBLOCK` 含む) で即座に購読者リストから破棄・ソケットクローズされる。
- **影響**: ネットワーク遅延やクライアント負荷時に SSE 接続が意図せず切断され、画面同期が失われる。
- **参照箇所**: [`Playmats/src/Main.kt` L763-L769](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/src/Main.kt#L763-L769)
- **検討案**: 再送キューの保持、または書き込み可能状態 (`FD_WRITE`) を確認してからの送信。

### 2-5. 単一グローバル GameState によるマルチセッション非対応
- **現状**: サーバープロセス内で単一の `val gameState` を共有。
- **影響**: 複数ユーザーや複数タブで独立したゲームセッションを進行できない。
- **参照箇所**: [`Playmats/src/Main.kt` L746](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/src/Main.kt#L746)
- **検討案**: `sessionId` をキーとする `ConcurrentMap<String, GameState>` によるセッション管理。

---

## 3. WebUI / フロントエンド (Playmats) の課題

### 3-1. Undo / Redo / Replay の未実装
- **現状**: UI に「戻る」「進む」ボタンやショートカット (Ctrl+Z / Ctrl+Y) が存在するが、バックエンド側で履歴スタックが保持されておらず常に無効。`/api/replay` も空配列を返すスタブ。
- **影響**: 手の巻き戻しや対戦棋譜の振り返りが不可。
- **参照箇所**: [`Playmats/src/Main.kt` L661-L662](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/src/Main.kt#L661-L662), [`Playmats/src/Main.kt` L991-L1000](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/src/Main.kt#L991-L1000), [`Playmats/src/index.html` L810-L811](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/src/index.html#L810-L811)
- **検討案**: 状態スナップショット履歴スタック (`history: MutableList<GameState>`) の導入。

### 3-2. 外部公式サイトへの画像直接依存
- **現状**: カード画像プレビューを `https://www.battlespirits.com/images/cardlist/...` から直接ロード。
- **影響**: オフライン環境や公式サーバーの仕様変更・アクセス制限時に画像が表示されない。
- **参照箇所**: [`Playmats/src/Main.kt` L526](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/src/Main.kt#L526), [`Playmats/src/index.html` L955](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/src/index.html#L955)
- **検討案**: ローカルキャッシュディレクトリ (`.bscards/images/` 等) からの配信プロキシ。

### 3-3. コア手動配分 UI における detail 文字列の再パース
- **現状**: バックエンドから構造化データ (`CoreEffectDto`) が届いているが、フロントエンドで正規表現 `parseDetailSources` を用いてテキストから移動元コア数を逆パースしている。
- **影響**: 表示文言の変更で手動配分UIのマッチングが壊れる。
- **参照箇所**: [`Playmats/src/index.html` L1146-L1180](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/Playmats/src/index.html#L1146-L1180)
- **検討案**: `option.core_effects` を直接照合するロジックへ移行。

---

## 4. データローダー / 環境依存の課題

### 4-1. ユーザープロファイルパスのハードコード
- **現状**: カード探索パスのフォールバック先に `"C:\\Users\\kyoya"` が直書き。
- **影響**: 他の開発者や別マシン環境でカードデータが見つからない。
- **参照箇所**: [`model/src/CardLoader.kt` L35](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/CardLoader.kt#L35)
- **検討案**: カレントディレクトリ基準の相対パス、または環境変数による統一解決。

### 4-2. 行パースによる YAML 解析の脆さ
- **現状**: YAML パーサーを用いず、行先頭の文字列判定 (`startsWith("cost:")` 等) でカードデータを抽出。
- **影響**: YAML のインデント・改行・コメントの揺らぎで抽出失敗する。
- **参照箇所**: [`model/src/CardLoader.kt` L68-L130](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/CardLoader.kt#L68-L130)
- **検討案**: KMP 対応の YAML パーサーライブラリの採用。

### 4-3. 未知カードの固定フォールバック
- **現状**: YAML が存在しないカードは全て「緑・甲魚・コスト3」固定。
- **影響**: 未対応カードが意図せず緑スピリット化し、シンボルや色計算が狂う。
- **参照箇所**: [`model/src/CardLoader.kt` L157-L169](file:///C:/Users/kyoya/home26/works/samples/2026/BSTools/model/src/CardLoader.kt#L157-L169)
- **検討案**: カード存在チェックエラーの明示と、属性未定ダミーカードの導入。

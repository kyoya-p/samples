# プロジェクト履歴

全17個のPOCおよびプロトタイプの実装履歴。

## POC 索引 (Index)

### 1. 基礎・ローカル開発フェーズ
- **[poc.1](./poc.1/)**: WebRTCの基本挙動確認。
- **[poc.2](./poc.2/)**: メディアストリーム基礎（カメラ・画面共有）。
- **[poc.3](./poc.3/)**: セキュアコンテキスト対応（HTTPS化）。
- **[poc.4](./poc.4/)**: 強制リレー検証（Force TURN）。

### 2. Azure クラウド・インフラ構築フェーズ
- **[poc.5](./poc.5/)**: Azure CLI 開発環境整備。
- **[poc.7](./poc.7/)**: App Service + ACI (Coturn) による初のクラウド実証。
- **[poc.8_ACS](./poc.8_ACS/)**: Azure Communication Services (ACS) リレー調査。

### 3. ACS 認証・SDK 調査フェーズ (Deep Dive)
- **[poc.az.acs1](./poc.az.acs1/)**: ACS リソース管理と Python SDK の基礎検証。
- **[poc.az.acs2](./poc.az.acs2/)**: Python による TURN 資格情報の動的取得検証。
- **[poc.az.acs3](./poc.az.acs3/)**: Kotlin/JS (Amper) と ACS Signaling の統合試作。

### 4. Kotlin/JS & ACS 統合フェーズ
- **[poc.9](./poc.9/)**: Kotlin/JS 移行後の接続エラー（404）分析。
- **[poc.10](./poc.10/)**: Identity 連携による ACS リレーの最終実証。

### 5. アプリケーション実装フェーズ
- **[BoardService](./BoardService/)**: ホワイトボード同期の初期実装。
- **[BoardService4](./BoardService4/)**: ACI Coturn リレーを統合したプロダクト構成。
- **[WhiteBoard5](./WhiteBoard5/)**: 安定版。ACI 最適化 Coturn 構築と DataChannel 同期。
- **[WhiteBoard5.1](./WhiteBoard5.1/)**: メンテナンス版。自動テスト導入、構造最適化。
- **[rtcsock.1](./rtcsock.1/)**: **(最新)** WebRTC DataChannel をトランスポート層として使用し、noVNC をクラウド/NAT越しに利用可能にする POC。

---

## 主要な技術的知見 (Findings)

### 1. ネットワーク・リレー
- **Azure ACS TURN 終了**: 2024年3月31日に終了。現在は ACI 上の Coturn 自前構築が必須。
- **Coturn 構成**: `--external-ip` 指定がないとリレー候補生成に失敗する。
- **経路検証**: `getStats()` で `Local[relay] <-> Remote[relay]` を確認する手法。

### 2. ブラウザ制限と実装
- **Secure Context**: `getDisplayMedia` は localhost/HTTPS 必須。
- **Candidate シリアル化**: `toJSON()` を使用しないとプロパティが欠落する。

### 3. テストと保守
- **シグナリング安定化**: テストごとのユニークな `room` 管理と `socket.id` フィルタリング。
- **接続判定**: `ConnectionState` だけでなく `ICEConnectionState` も成否判定に活用。

### 4. VNC 連携 (rtcsock)
- **WebSocket 抽象化**: noVNC の `RFB.js` が期待する WebSocket オブジェクトを WebRTC DataChannel でラップして偽装 (`fakeWS`) することで、既存ライブラリを無改造で WebRTC 化。
- **ヘッドレス・ブリッジ**: サーバーサイドでの WebRTC スタック構築を避け、Playwright (Chromium) をエージェントとして動かすことで安定した VNC ↔ WebRTC 変換を実現。

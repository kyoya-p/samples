# 概要
Azure ACS を使用したWebRTC のサーバとそれ用のWebクライアントを作成
Webクライアントは下記それぞれの接続方
- TURN: インターネット間通信 (Coturn サーバーを使用)
- STUN: NAT/PROXY経由のグローバルIP通信
- HOST: P2P IP接続


# 実装のポイント
本実装では、Azure ACS のシグナリング機能を維持しつつ、廃止された ACS リレーサービスの代わりに ACI 上に構築した **Coturn サーバー** を使用して WebRTC PeerConnection を確立。
データ同期（描画点）は、P2P接続が確立されている場合は **WebRTC DataChannel** を通じて実行される。
- 
- **Server**: Express サーバーにて、ACS の Identity トークン発行と同時に、構築した Coturn サーバーの認証情報を `/ice-servers` で提供。
- **Client (Kotlin/JS)**: `RTCPeerConnection` と `RTCDataChannel` を実装。接続確立時にアクティブな ICE 候補ペアを表示するデバッグ機能を搭載。
- **TURN Server**: `coturn/coturn` イメージをベースに、UDP 3478 ポートおよび長期認証 (lt-cred-mech) を有効化して ACI に配備。

# 環境
- OS: Ubuntu 24.04
- mise: 2026.2.11

以下miseプロジェクト環境に自動設定される
- azure-cli 
- java 21
- amper 

# セットアップ
```bash
mise trust
mise install
mise run setup:amper
az login  # ブラウザが開きlogin、その後、使用subscriptionを指定
# az account list --output table # subscriptionのリスト
```

# デプロイ
Azure にサービス（App Server & TURN Server）を配備
```bash
mise run deploy
```

## 3. 検証
- **TURN 強制接続テスト**: 「Force TURN (Relay only)」を有効にし、ログに `Selected Path: Local[relay] <-> Remote[relay]` が表示されることを確認。
- **DataChannel 同期**: 複数ブラウザタブ間でのリアルタイム描画同期を確認。
- **スクリーンショット**: `.playwright-mcp/` 内の画像で操作結果と接続ログの状態をエビデンスとして保持。


# 知見 (Findings)
- **Azure ACS Network Traversal の廃止**: 2024年3月31日をもって Azure ACS の TURN サービスは完全に終了しており、現在は 404 (Front Door Error) が返されるため利用不能。本 POC では ACI 上に Coturn を構築することで代替。
- **Coturn on ACI の構成**: ACI で Coturn を動かす際は、`--external-ip` に ACI のパブリック IP を明示的に指定する必要がある。これを怠ると内部 IP (`192.168.x.x`) で応答してしまい、外部からのリレー候補生成に失敗する。
- **WebRTC 接続経路の検証**: `peerConnection.getStats()` を解析し、`Selected Path: Local[relay] <-> Remote[relay]` というログを出力することで、収集した候補（Candidate）の中から実際に TURN 経路が選択されたことを厳密に実証済み。
- **Kotlin/JS のブラウザテスト手法**: Amper でビルドされた Kotlin/JS アプリケーションを Playwright で自動テストし、`connected` 状態や統計情報をキャプチャ可能。




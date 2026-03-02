# 概要
Azure ACS を使用したWebRTC のサーバとそれ用のWebクライアント(KMP)を作成
Webクライアントは下記それぞれの接続方式
- インターネット間(TURN経由)通信 (Azure ACS Network Traversalを使用)
- NAT/PROXY経由のグローバルIP通信
- LAN IP接続

本実装では、Azure ACSのCommunicationRelayClientを使用してICEサーバー情報を取得し、WebRTC PeerConnectionを確立
データ同期（描画点）は、P2P接続が確立されている場合は **WebRTC DataChannel** を通じて実行される

# 知見 (Findings)
- **Azure ACS TURN 資格情報の取得と認証**: `CommunicationRelayClient` を使用して TURN 資格情報を取得する際は、単なる接続文字列だけでなく、`CommunicationIdentityClient` で作成した **`CommunicationUserIdentifier` (Identity)** を引数に渡す必要があります。これを行わない場合、Azure の内部ゲートウェイ（Front Door 等）から 404 Error が返され、リレー設定の取得に失敗します。
- **Kotlin/JS のブラウザテスト手法**: Amper でビルドされた Kotlin/JS アプリケーション (`.mjs`) をブラウザで自動テストする場合、**Playwright** 等のツールを用いてブラウザを起動し、グローバルに `suite` や `test` (QUnit/Jasmine 互換) のモックインターフェースを注入することで、`kotlin-test` の実行結果をホスト側の Node.js でキャプチャ可能です。
- **WebRTC DataChannel による同期の実証**: Azure ACI 上にデプロイされた環境において、複数クライアント（ブラウザタブ）間で **DataChannel が正常にオープン（"DataChannel opened"）** されることをコンソールログおよび Playwright テストで確認しました。Socket.io をシグナリングに使用し、DataChannel を介したリアルタイムな描画同期が可能な状態であることを実証済みです。

- **Azure ACS Network Traversal のステータス**: 一時的にサービス廃止の懸念あり、Identity 連携を正しく行うことで現在も TURN サービスが利用可能であることを実証。

# 実装のポイント
- **Server**: `@azure/communication-identity` と `@azure/communication-network-traversal` を使用して、認証済みの ICE サーバー設定を `/ice-servers` エンドポイントで提供。
- **Client (Kotlin/JS)**: `RTCPeerConnection` と `RTCDataChannel` を実装。シグナリングには既存のSocket.ioを使用。
- **UI**: 「Connect P2P (WebRTC)」ボタンでP2P接続を手動開始可能。


# 環境
- OS: Ubuntu 24.04
- mise: 2026.2.11
- azure-cli (miseを使用し導入)
- java (miseを使用し導入)
- amper (mise taskを使用し導入)

# セットアップ
```bash
mise trust
mise install
mise run setup:amper
az login  # or # az login --use-device-cod 
# az account list --output table
# az account set --subscription "サブスクリプション名またはID"
```

# リソース作成 & ビルド & デプロイ
以下の手順で、Azure ACSのリソース作成、コンテナイメージのビルド(ACR)、およびACIへのデプロイを実行。
```bash
mise run deploy  # build/depolyを実行
```

## 3. 検証
- Azure CloudとPlaywrightブラウザによる通信テスト
- デプロイで表示されたURLを開く
- スクショ(playwright用の一時フォルダに格納)で操作の結果を確認
- 繰り返し修正市テストする様指示ない場合、判断結果と対応方針を表示しテスト終了

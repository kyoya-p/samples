# WebRTC Stream PoC (poc-4)

このプロジェクトは、WebRTC を使用したリアルタイムメディアストリーム（カメラ映像および画面共有）の相互通信を実現する概念実証（PoC）です。
サーバーは HTTPS (TLS) で動作し、内蔵 TURN サーバーによるリレー通信の検証機能を備えています。

## 機能
- **カメラ映像の送信**: `getUserMedia` を使用してカメラとマイクを取得。
- **画面共有 (Window キャプチャ)**: `getDisplayMedia` を使用して任意のウィンドウや画面を配信。
- **フォールバック機能**: カメラがない環境向けに、Canvas による動的ダミーストリームを自動生成。
- **P2P / Relay 通信**: WebSocket シグナリングサーバーを介して SDP/ICE を交換。
- **TURN強制モード (New)**: UI上のチェックボックスで `iceTransportPolicy: 'relay'` を強制し、TURNサーバー経由の通信をデバッグ可能。
- **動的トラック切り替え**: 通信を維持したまま、カメラから画面共有への切り替えに対応。

## 実行手順

### 1. 依存関係のインストール
```bash
cd poc.4
npm install
```

### 1b. 自己署名証明書の生成
HTTPS 通信のために証明書（`key.pem`, `cert.pem`）を `poc.4` 直下に作成します。
(Windows 環境などで openssl がない場合は `node-selfsigned` 等を利用して生成してください)

```bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -sha256 -days 365 -nodes -subj "/CN=localhost"
```

### 2. サーバーの起動
```bash
node src/server.js
```
- **HTTPS サーバー**: `https://localhost:8080`
- **TURN サーバー**: Port `3478` (認証: user/password)
- **シグナリング**: WebSocket Secure port:8080

### 3. クライアントの操作
2つのブラウザタブで `https://localhost:8080` を開きます。
※自己署名証明書のため、ブラウザで警告が表示されます。「詳細設定」→「localhost にアクセスする（安全ではありません）」を選択して進めてください。

#### 手順 A (送信側)
1. **Force TURN (Relay)** をチェック (任意: TURN経由を強制する場合)。
2. **1. Start Camera** または **1b. Share Window** をクリックして配信ソースを準備。
3. **2. Connect Signaling** をクリックしてシグナリングサーバーに接続。
4. **3. Call (Create Offer)** をクリックして接続を開始。

#### 手順 B (受信側)
1. **Force TURN (Relay)** をチェック (任意: 送信側と合わせることを推奨)。
2. **2. Connect Signaling** をクリックして待機。
3. 送信側が Call を実行すると、自動的に映像が表示されます。

## 検証結果 (2026/02/14)
- **TURNリレー確認**: `Force TURN` 有効時、ブラウザログに `ICE Candidate: relay (...)` のみが出現し、TURNサーバーログにパケット転送記録 (`Receiving`/`Sending`) が確認された。
- **映像通信**: リレー経由でも遅延なく映像・音声が到達することを確認。

## 技術スタック
- **Frontend**: Vanilla JS (WebRTC API)
- **Backend**: Node.js, `ws` (WebSocket), `node-turn` (TURN Server)
- **Signaling**: 自作の簡易 WebSocket シグナリング (HTTPS 共有型)

## 注意事項
- 画面共有 (`getDisplayMedia`) は、セキュアなコンテキスト (localhost または HTTPS) でのみ動作します。
- 物理カメラがない環境では、自動的に「動くオブジェクトと時刻を表示するダミーストリーム」が生成されます。

# WebRTC Stream PoC (poc-2)

このプロジェクトは、WebRTC を使用したリアルタイムメディアストリーム（カメラ映像および画面共有）の相互通信を実現する概念実証（PoC）です。

## 機能
- **カメラ映像の送信**: `getUserMedia` を使用してカメラとマイクを取得。
- **画面共有 (Window キャプチャ)**: `getDisplayMedia` を使用して任意のウィンドウや画面を配信。
- **フォールバック機能**: カメラがない環境向けに、Canvas による動的ダミーストリームを自動生成。
- **P2P 通信**: WebSocket シグナリングサーバーを介して SDP/ICE を交換し、ブラウザ間で直接映像を転送。
- **動的トラック切り替え**: 通信を維持したまま、カメラから画面共有への切り替えに対応。

## 実行手順

### 1. 依存関係のインストール
```bash
cd poc-2
npm install
```

### 2. サーバーの起動
```bash
node src/server.js
```
- HTTP サーバー: `http://localhost:8080`
- TURN サーバー: `port 3478` (認証: user/password)
- シグナリング: WebSocket (同一ポート)

### 3. クライアントの操作
2つのブラウザタブで `http://localhost:8080` を開きます。

#### 手順 A (送信側)
1. **1. Start Camera** または **1b. Share Window** をクリックして配信ソースを準備。
2. **2. Connect Signaling** をクリックしてシグナリングサーバーに接続。
3. **3. Call (Create Offer)** をクリックして接続を開始。

#### 手順 B (受信側)
1. **2. Connect Signaling** をクリックして待機。
2. 送信側が Call を実行すると、自動的に映像が表示されます。

## 技術スタック
- **Frontend**: Vanilla JS (WebRTC API)
- **Backend**: Node.js, `ws` (WebSocket), `node-turn` (TURN Server)
- **Signaling**: 自作の簡易 WebSocket シグナリング

## 注意事項
- 画面共有 (`getDisplayMedia`) は、セキュアなコンテキスト (localhost または HTTPS) でのみ動作します。
- 物理カメラがない環境では、自動的に「動くオブジェクトと時刻を表示するダミーストリーム」が生成されます。

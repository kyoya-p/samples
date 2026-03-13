# WebRTC サーバー付き接続テスト (poc.11)

シグナリングサーバー（Socket.io）と TURN サーバー（Coturn）を備えた WebRTC P2P 接続テスト環境。

## 構成
- **Frontend:** Vanilla JS (RTCPeerConnection + Socket.io-client) - `server/public`
- **Signaling Server:** Node.js (Express + Socket.io) - `server`
- **TURN Server:** Coturn (Docker) - `turn`
- **Browser Clients:** Playwright (Headfull in Docker with noVNC) - `Dockerfile.client`

## アプリ設計

### 1. 全体アーキテクチャ
本プロジェクトは、WebRTC のライフサイクル（シグナリング、交渉、接続、メディア転送）を完結させるための構成となっている。
- **Signaling Layer:** Socket.io を用いたルームベースのメッセージング。
- **Media Layer:** `HTMLCanvasElement.captureStream()` を利用した仮想ビデオソース。
- **Network Layer:** STUN/TURN を介した ICE プロトコルによる NAT 越え。

### 2. シグナリング・フロー
自動シグナリングのシーケンスは以下の通り：
1. **Join:** クライアントが指定のルームに参加。
2. **Detection:** ルームに後からユーザーが参加すると、既存ユーザーに `user-joined` が通知される。
3. **Negotiation:** 
   - 既存ユーザー（先にいた側）が `Offer` を作成し、Socket.io 経由で送信。
   - 新規ユーザーが `Offer` を受け取り、`Answer` を作成して返信。
4. **ICE Trickle:** 交渉と並行して、取得された ICE Candidate を随時交換し、最適な経路を確立する。

### 3. 仮想メディアソース (Qix Animation)
実デバイスの代わりに、ブラウザ上でリアルタイム描画される「Qix」アニメーションをビデオトラックとして使用している。
- Canvas への描画内容を 30fps でストリーム化。
- ビデオ要素をクリックすることで、新しい描画ラインを動的に追加可能。

### 4. ネットワーク戦略
- **ICE Servers:** Google のパブリック STUN と、Docker 内の Coturn (TURN) を併用。
- **Relay:** `EXTERNAL_IP` 環境変数を使用して、TURN サーバーがリレー用のアドレスを適切に広報できるように設計。

### クライアントUI
- 要素をアダプティヴに配列: コンソール、Localビデオ、リモートビデオ、ログ
- コンソール: WebRTCプロトコル検証のためステップ実行可能な操作パネル。以下の要素からなる
    - Session:
        - RoomID入力: default='room1'
        - Connect ボタン
        - ICE候補リスト
            - HOSTチェックボックス: OffにするとHOST候補を選択から除外

        - Localビデオ: 動画発信Qixエフェクト。
        - リモートビデオ
        - ログ: 動作ログを表示
        - Siganaling Sessionの内容
        - その他動作内容

        ## 起動方法
        Docker Compose を使用して一括起動します。`mise` がインストールされている場合は、タスクから簡単に実行できます。

        ```bash
        cd poc.11
        # mise を使う場合
        mise run up

        # または直接 docker-compose を使う場合
        docker-compose up --build
        ```

        ### 利用可能なタスク (mise)
        - `mise run up`: サーバー群とクライアントコンテナを起動
        - `mise run down`: サーバー群を停止・削除
        - `mise run logs`: リアルタイムログを表示
        - `mise run ps`: コンテナのステータスを確認

        1. ブラウザで `http://localhost:3000` を開くか、コンテナ化されたクライアントを VNC で確認します。
        - **Client 1 (VNC):** `http://localhost:8081/vnc.html?autoconnect=true`
        - **Client 2 (VNC):** `http://localhost:8082/vnc.html?autoconnect=true`
        2. 両方のタブ/クライアントで同じ **Room Name** を入力して **Join** をクリック。
        3. 自動的にシグナリングが開始され、P2P 接続が確立される。
        4. 接続後、相手の「Qix」アニメーションが表示される。

        ## 補足
        - `EXTERNAL_IP` はデフォルトで `127.0.0.1` に設定されています。
        - ローカルネットワーク外でテストする場合は `docker-compose.yml` の `EXTERNAL_IP` を実際のパブリック IP に書き換える必要があります。

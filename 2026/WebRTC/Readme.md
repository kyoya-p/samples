

```mermaid
sequenceDiagram
    participant A as Client-A
    box Cloud WebRTC Service
        participant Srv as STUN<br>(Session Traversal<br>Utilities for NAT)
        participant SIG as Signaling<br>Service
        participant turn as TURN<br>(Traversal Using <br>Relays around NAT)
    end
    participant B as Client-B

    Note over A, B: ICE(Interactive Connectivity Establishment)
    par
        A->>Srv: Binding/Allocate Request
    and
        B->>Srv: Binding/Allocate Request
    end

    A->>SIG: Offer(SDP)<br>SDP:Session Description Protocol
    SIG->>B: Offer(SDP)
    B-->>SIG: Answer(SDP)
    SIG-->>A: Answer(SDP)

    Note over A, B: クライアント間通信

    alt Host接続
        A->>B: Local IP通信(Data)
    else STUN接続
        A->>B: Global IP通信(Data)
    else リレー接続
        A ->> turn: 送信(Data)
        turn ->> B: 送信(Data)
        B -->> turn: 受信(Data)
        turn -->> A:  受信(Data)
    end
```

# POC概要
- poc.1: WebSocket を使用した基本的なシグナリングによる P2P メッセージ送受信の基礎。
- poc.2: カメラ映像（getUserMedia）のリアルタイムストリーミング。
- poc.3: P2P が失敗する場合に備えた、内蔵 TURN サーバーによるリレー通信の実装。
- poc.4: カメラ/画面共有の切り替えおよび、TURN 強制モード（iceTransportPolicy: relay）の検証機能。
- poc.5: `mise` を利用した Azure CLI (az) 実行環境のセットアップ自動化。
- poc.7: Azure App Service (Node.js) と Azure Container Instances (coturn) への完全デプロイ。

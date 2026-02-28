# WebRTC POC

# 概要
- poc.1: WebSocket を使用した基本的なシグナリングによる P2P メッセージ送受信の基礎。
- poc.2: カメラ映像（getUserMedia）のリアルタイムストリーミング。
- poc.3: P2P が失敗する場合に備えた、内蔵 TURN サーバーによるリレー通信の実装。
- poc.4: カメラ/画面共有の切り替えおよび、TURN 強制モード（iceTransportPolicy: relay）の検証機能。
- poc.5: `mise` を利用した Azure CLI (az) 実行環境のセットアップ自動化。
- poc.7: Azure App Service (Node.js) と Azure Container Instances (coturn) への完全デプロイ。


```mermaid
sequenceDiagram
    participant A as クライアントA
    participant STUN as STUNサーバー
    participant SIG as シグナリングサーバー
    participant B as クライアントB

    Note over A, STUN: 自身のパブリックIPを調査
    A->>STUN: Binding Request
    STUN-->>A: Binding Success (パブリックIP/Port)

    Note over A, B: シグナリングでIP情報を交換
    A->>SIG: SDP (自身のパブリックIP)
    SIG->>B: SDP (AのパブリックIP)
    B->>SIG: SDP (自身のパブリックIP)
    SIG->>A: SDP (BのパブリックIP)

    Note over A, B: P2P直接通信の確立
    A<->>B: メディアデータ (UDP直接送信)
 ```


 ```mermaid
 sequenceDiagram
     participant A as クライアントA
     participant ACS as ACS (TURNサーバー)
     participant SIG as シグナリングサーバー
     participant B as クライアントB

     Note over A, ACS: 1. リレー用のアドレスを確保
     A->>ACS: Allocate Request (認証情報付)
     ACS-->>A: Allocate Success (リレーIP/Port)

     Note over A, B: 2. シグナリングでリレー情報を交換
     A->>SIG: SDP (ACSリレーのアドレス)
     SIG->>B: SDP (Aのリレーアドレス)
     B->>SIG: SDP (自身の候補)
     SIG->>A: SDP (Bの候補)

     Note over A, B: 3. ACSを中継した通信
     A->>ACS: 送信データ (Send Indication)
     ACS->>B: データ転送 (Data Indication)
     B->>ACS: 返信データ
     ACS->>A: データ転送
```

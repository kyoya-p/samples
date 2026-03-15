# WebRTC 接続シーケンス (RFC 8445 準拠)

ICE (Interactive Connectivity Establishment) における主要な接続パターン別の通信確立フロー。
## 0. ICE Candidate交換
WebRTC接続を確立するために、SDP (Session Description) や ICE Candidate などの制御情報をピア間で交換するプロセス。

```mermaid
sequenceDiagram
    participant A as Client-A (controller)
    participant S as Signaling Server
    participant B as Client-B

    A->>S: セッション確立
    B->>S: セッション確立

    Note over A, B: Offer/Answer 交換
    A->>S: Offer(SDP-A)
    S->>B: Offer(SDP-A)
    B->>S: Answer(SDP-B)
    S->>A: Answer(SDP-B)
    
    Note over A, B: Candidate 交換 (Trickle ICE)
    A->>S: ICE Candidate (A)
    S->>B: ICE Candidate (A)
    B->>S: ICE Candidate (B)
    S->>A: ICE Candidate (B)
```

## 1. Host-to-Host (ローカルP2P/UDP)
同一ネットワーク内などで、直接パケットが到達可能な場合のフロー。

```mermaid
sequenceDiagram
    participant A as Client-A
    participant B as Client-B
     
    Note over A, B: ICE Candidate交換
     
    A->>B: STUN Binding Request (Check)
    B-->>A: STUN Binding Response (Success)
    
    B->>A: STUN Binding Request (Check)
    A-->>B: STUN Binding Response (Success)
    
    Note right of A: Controlling側がペアを選択
    A->>B: STUN Binding Request (USE-CANDIDATE)
    B-->>A: STUN Binding Response (Success)
    
    Note over A, B: 双方向P2P通信確立 (UDP)
```

## 2. Srflx-to-Srflx (NAT越えP2P / STUN)
異なるネットワーク間で、NATの「ホールパンチング」により直接通信を試みるフロー。

```mermaid
sequenceDiagram
    participant A as Client-A
    participant NAT_A as NAT-A
    participant STUN as STUN Server
    participant NAT_B as NAT-B
    participant B as Client-B

    Note over A, B: ICE Candidate交換
    A->>STUN: STUN Binding Request
    STUN-->>A: STUN Binding Response (XOR-MAPPED-ADDRESS)
    
    Note over A, B: srflx候補を交換
    
    Note over A, B: 接続確認 (Connectivity Checks)
    A->>NAT_A: STUN Binding Request (to B's srflx)
    NAT_A->>NAT_B: STUN Binding Request (Hole Punching)
    Note right of NAT_B: NAT_BがAからのパケットを許可
    
    B->>NAT_B: STUN Binding Request (to A's srflx)
    NAT_B->>NAT_A: STUN Binding Request
    
    NAT_A-->>B: STUN Binding Response
    NAT_B-->>A: STUN Binding Response
    
    Note over A, B: P2P通信確立 (UDP)
```

## 3. Relay (TURN経由 / UDP)
Symmetric NAT（対称NAT）間など、直接通信が不可能な場合にサーバーを経由するフロー。

```mermaid
sequenceDiagram
    participant A as Client-A
    participant TURN as TURN Server
    participant B as Client-B

    Note over A, TURN: 収集 (Gathering)
    A->>TURN: TURN Allocate Request
    TURN-->>A: TURN Allocate Response (XOR-RELAYED-ADDRESS)
    Note left of A: relay候補を取得
    
    Note over A, B: シグナリングでrelay候補を交換
    
    Note over A, TURN: 権限設定 (Permissions)
    A->>TURN: CreatePermission Request (for B)
    TURN-->>A: CreatePermission Response
    
    Note over A, B: 通信 (Data Relay)
    A->>TURN: Send Indication (Data to B)
    TURN->>B: Data (UDP)
    B-->>TURN: Data (UDP)
    TURN-->>A: Data Indication (Data from B)
```

## 4. Relay (TURN経由 / TCP・TLS)
UDPが完全に遮断されている環境で、TCPまたはTLS(Port 443)を介してリレーする最終手段。

```mermaid
sequenceDiagram
    participant A as Client-A
    participant TURN as TURN Server
    participant B as Client-B

    Note over A, TURN: TCP/TLS接続確立 (Handshake)
    A->>TURN: TCP SYN / TLS Client Hello
    TURN-->>A: TCP ACK / TLS Server Hello
    
    Note over A, TURN: TURN Allocate over TCP
    A->>TURN: TURN Allocate Request
    TURN-->>A: TURN Allocate Response
    
    Note over A, B: シグナリングで候補交換
    
    Note over A, B: データ転送 (Framed Data)
    A->>TURN: TURN Send (Data to B)
    Note right of TURN: UDPに変換してBへ
    TURN->>B: Data (UDP)
    B-->>TURN: Data (UDP)
    Note left of TURN: TCP/TLSフレームに包んでAへ
    TURN-->>A: Data Indication (Data from B)
```

---
ソース: 
- [RFC 8445: Interactive Connectivity Establishment (ICE)](https://datatracker.ietf.org/doc/html/rfc8445)
- [RFC 8829: JavaScript Session Establishment Protocol (JSEP)](https://datatracker.ietf.org/doc/html/rfc8829)

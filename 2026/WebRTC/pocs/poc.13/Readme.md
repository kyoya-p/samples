# WebRTC Text Chat POC (poc.13)

WebRTC `RTCDataChannel` を利用した P2P テキストチャット環境。

## 構成
- **Frontend:** Vanilla JS (`RTCPeerConnection` + `RTCDataChannel` + `Socket.io-client`) - `server/public`
- **Signaling Server:** Node.js (Express + Socket.io) - `server`
- **TURN Server:** Coturn (Docker) - `turn`
- **Browser Clients:** Playwright (Headfull in Docker with noVNC) - `Dockerfile.client`

## アプリ設計

### 1. データチャネル層
本プロジェクトでは、ビデオ/音声トラックの代わりに `RTCDataChannel` を使用する。
- **Initiator (先にいた側):** `pc.createDataChannel("chat")` を呼び出し、DataChannel を作成。
- **Receiver (後から来た側):** `pc.ondatachannel` イベントでリモートの DataChannel を受信。
- 両者が DataChannel の `onopen` を受信した時点でテキストの送受信が可能になる。

### 2. シグナリング・フロー
1. **Join:** クライアントが指定のルームに参加。
2. **Detection:** ルームに後からユーザーが参加すると、既存ユーザーに `user-joined` が通知される。
3. **Negotiation:** 
   - 既存ユーザーが `Offer` を作成（この時 DataChannel を事前作成）し、送信。
   - 新規ユーザーが `Offer` を受け取り、`Answer` を作成して返信。
4. **ICE Trickle:** 交渉と並行して、取得された ICE Candidate を交換。

### 3. UI/UX
- メッセージ履歴を表示するチャットウィンドウ。
- 送信ボタンおよび Enter キーによるメッセージ送信。
- DataChannel の状態（open/closed）をリアルタイムにバッジで表示。

## 修正
テスト結果 `./test`を参照し修正。
修正内容を `./test/MMDD-HHMMSS.fix-report.md` に生成
テストは別のAIセッションで実施するので不要

## ローカルテスト

各テストの実行内容とその結果のを比較し、内容が期待通りではないことを証明
テスト環境として docker-compose.test.yml を起動
テスト用のコードのみ修正可とする
プログラム修正は別AIセッションで実施するので不要
結果を `test/MMDD-HHMMSS.test-report.md` に生成

```bash
mise run up
```
### Test0
- nat1 -> server : TCP通信が可能
- nat1 -> server : UDP通信が可能
- nat1 <- server : TCP通信が不可能
- nat1 <- server : UDP通信が不可能

- pxy1 -> server : TCP通信が不可能
- pxy1 -> server : UDP通信が不可能
- pxy1 -> server : Proxyを通じてHTTP通信が可能
- pxy1 <- server : TCP通信が不可能
- pxy1 <- server : UDP通信が不可能


### Test1.1
nat1にクライアントを2個起動し、通信テスト
ssを確認

### Test1.2
pxy1にクライアントを2個起動し、通信テスト
ssを確認

### Test2.1
nat1とnat2にクライアントを1個ずつ起動し、通信テスト
ssを確認

### Test2.2
pxy1とpxy2にクライアントを1個ずつ起動し、通信テスト
ssを確認

### Test2.3
nat1とpxy1にクライアントを1個ずつ起動し、通信テスト
ssを確認


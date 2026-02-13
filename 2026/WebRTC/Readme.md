# WebRTC POC

# 概要
WebRTC習得目的。下記挙動を確認したい。
a. Client-AとClient-Bが同じLANにある場合、データ通信はサーバを介さない (ICE: host/srflx)
b. Client-AとClient-Bが異なるLANにある場合、データ通信はサーバを介す (ICE: relay)

# 内容
- App & シグナリングサーバ: port 8080
- TURN/STUN サーバ (node-turn): port 3478

# 手順
1. サーバを起動する。
```sh
cd src
npm i
node server.js
```
2. クライアント(ブラウザ)で http://localhost:8080 を開く。
3. `Connect` -> `Create Offer` (Client A)
4. ログで `ICE Candidate: relay` が出ていることを確認する（Case bの場合）。
   ※外部PCから接続して `relay` を確認する場合は、`index.html` 内の `turn:localhost:3478` をサーバの実際のIPアドレスに変更してください。

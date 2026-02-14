# WebRTC POC.1

# 概要
WebRTC習得目的。下記挙動を確認したい。
- a. [Client-A/Bが直接通信できる場合]TURNを介さない (ICE: host/srflx)
- b. [Client-A/Bが直接通信できない場合]TURNを介して通信 (ICE: relay)

# 内容
- App & シグナリングサーバ: port 8080
- TURN/STUN サーバ (node-turn): port 3478

# 手順
1. サーバを起動する。
```sh
npm i
node src/server.js
```
2. クライアント(ブラウザ)で http://localhost:8080 を開く。
3. `Connect` -> `Create Offer` (Client A)
4. ログで `ICE Candidate: relay` が出ていることを確認する（Case bの場合）。

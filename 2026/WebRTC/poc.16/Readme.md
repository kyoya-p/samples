# WebRTC Data Channel SDK

# プロジェクト目標

- WebRTCを使用した双方向常時通信を行うデータチャネルライブラリ`webrtc-channel.js`を作成。
  - 実行環境はnode(ブラウザではない)。
- 専用のSignaling serverを作成。Dockerコンテナとして実装。
  - 実行環境は制限しない
- テスト用の仮想Network環境をDockerコンテナを使用して実装。
- テスト用スクリプトを生成

# テスト環境
2個のクライアントが通信

- nat1 -> server : TCP通信が可能
- nat1 -> server : UDP通信が可能
- nat1 <- server : TCP通信が不可能
- nat1 <- server : UDP通信が不可能

- pxy1 -> server : TCP通信が不可能
- pxy1 -> server : UDP通信が不可能
- pxy1 -> server : Proxyを通じてHTTP通信が可能
- pxy1 <- server : TCP通信が不可能
- pxy1 <- server : UDP通信が不可能

# テスト環境
下記パターンで検証

- test:
    - nat1 <- nat2
    - nat1 <- pxy1
    - pxy1 <- pxy2

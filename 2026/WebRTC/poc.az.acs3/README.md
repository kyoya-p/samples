# 概要
Azure ACS を使用したWebRTC のサーバとそれ用のWebクライアント(KMP)を作成
Webクライアントは下記それぞれの接続方式
- インターネット間(TURN経由)通信
- NAT/PROXY経由のグローバルIP通信
- LAN IP接続

# 環境
- OS: Ubuntu 24.04
- mise: 2026.2.11
- azure-cli (miseを使用し導入)
- java (miseを使用し導入)
- Amper (mise taskを使用し導入)

# セットアップ
```bash
# ツール類のインストール
mise install


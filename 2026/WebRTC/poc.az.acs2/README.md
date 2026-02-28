# Azure Communication Services (ACS) TURN Service Setup

ACS を WebRTC 用の TURN サーバーとして利用するためのリポジトリ。

## 前提条件

- [Azure CLI](https://learn.microsoft.com/ja-jp/cli/azure/install-azure-cli)
- [mise](https://mise.jdx.dev/) (環境変数・タスク管理)
- [uv](https://docs.astral.sh/uv/) (Python パッケージ管理)

## セットアップ手順

### 1. ACS リソースの作成
Azure 上に ACS リソースを作成し、接続文字列を `.env` に保存する。

```bash
mise run create:acs
```

### 2. TURN 資格情報の取得
一時的な TURN/STUN サーバーの資格情報（URL, Username, Password）を JSON 形式で取得する。

```bash
mise run get:turn
```

## 主要なファイル

- `get_turn_credentials.py`: ACS SDK を使用して TURN 資格情報を取得するスクリプト。
- `mise.toml`: リソース作成、削除、資格情報取得のタスク定義。
- `pyproject.toml`: `azure-communication-networktraversal` 等の依存ライブラリ定義。

## 注意事項

- ACS の TURN 資格情報は有効期限付き（デフォルト 48 時間）。
- この Python スクリプトはバックエンドサーバー等で実行し、フロントエンド（WebRTC クライアント）に `iceServers` 情報を渡す用途で利用する。

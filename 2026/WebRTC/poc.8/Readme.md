# WebRTC Stream PoC on Azure (poc.7)

このプロジェクトは、`poc.4` の WebRTC ストリーミング機能を Azure 環境へデプロイした概念実証（PoC）です。
Azure App Service（シグナリング）と Azure Container Instances（TURN）を組み合わせて構成されています。

## デプロイ情報 (2026/02/16)
- **URL**: [https://app-webrtc7-958948618.azurewebsites.net](https://app-webrtc7-958948618.azurewebsites.net)
- **TURN サーバー**: `turn:turn7-958948618.centralus.azurecontainer.io:3478`
- **TURN 認証**: `user` / `password`
- **リソースグループ**: `rg-webrtc7-poc` (Location: centralus)

## 構成
- **Signalig Server**: Azure App Service (Node.js 20 LTS)
- **TURN Server**: Azure Container Instances (Image: `instrumentisto/coturn`)
- **Infrastructure as Code**: `deploy.ps1` (Azure CLI 使用)

## 使用方法

### 1. 環境準備
`mise` を使用して Azure CLI を実行できる環境を構築します。
```powershell
mise trust
az login --tenant DIRECTORY_ID
mise run setup
```

### 2. デプロイ
ソースコードを修正した場合は、以下のコマンドで Azure へ反映できます。
```powershell
mise run deploy
```

### 3. ブラウザでの操作
1. `https://app-webrtc7-958948618.azurewebsites.net` を2つのタブで開きます。
2. **Force TURN (Relay)** をチェック（任意：TURN経由を強制する場合）。
3. **1. Start Camera** または **1b. Share Window** をクリック。
4. **2. Connect Signaling** をクリック。
5. **3. Call (Create Offer)** をクリック。

## 削除方法
検証が終了したら、リソースグループを削除して課金を停止してください。
```powershell
mise run destroy
```

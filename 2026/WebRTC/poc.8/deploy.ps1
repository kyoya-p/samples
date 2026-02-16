# Azure デプロイスクリプト

if (!(Get-Command az -ErrorAction SilentlyContinue)) {
    Write-Error "Azure CLI (az) が見つかりません。先に 'mise run setup' を実行してください。"
    exit 1
}

# 変数設定
$resourceGroup = "rg-webrtc-poc"
$location = "centralus"
$suffix = Get-Random
$appName = "app-webrtc-$suffix"
$planName = "plan-webrtc-$suffix"
$turnDnsLabel = "turn-$suffix"

Write-Host "1. リソースグループを作成中: $resourceGroup (Location: $location)..." -ForegroundColor Cyan
az group create --name $resourceGroup --location $location

# TURN サーバー (ACI) のデプロイ
Write-Host "2. TURN サーバー (ACI) をデプロイ中: $turnDnsLabel..." -ForegroundColor Cyan
$turnUser = "user"
$turnPass = "password"
# 失敗しても続行するようにするが、ログは出す
az container create --resource-group $resourceGroup --name $turnDnsLabel --image instrumentisto/coturn `
  --dns-name-label $turnDnsLabel --ports 3478 --protocol UDP --os-type Linux --cpu 1 --memory 1.5

$turnUrl = "turn:$turnDnsLabel.$location.azurecontainer.io:3478"

# シグナリングサーバー (App Service) の作成
Write-Host "3. App Service プランを作成中..." -ForegroundColor Cyan
az appservice plan create --name $planName --resource-group $resourceGroup --location $location --sku F1 --is-linux

Write-Host "4. Web App (App Service) を作成中: $appName..." -ForegroundColor Cyan
az webapp create --name $appName --resource-group $resourceGroup --plan $planName --runtime "NODE:20-lts"

# 環境変数の設定
Write-Host "5. アプリケーション設定 (環境変数) を構成中..." -ForegroundColor Cyan
az webapp config appsettings set --name $appName --resource-group $resourceGroup --settings `
  TURN_SERVER_URL="$turnUrl" `
  TURN_USERNAME="$turnUser" `
  TURN_PASSWORD="$turnPass" `
  PORT=80 `
  SCM_DO_BUILD_DURING_DEPLOYMENT=true

# コードのデプロイ
Write-Host "6. コードをデプロイ中..." -ForegroundColor Cyan
if (Test-Path "deploy.zip") { Remove-Item "deploy.zip" }
Compress-Archive -Path "src", "package.json" -DestinationPath "deploy.zip" -Force
az webapp deploy --name $appName --resource-group $resourceGroup --src-path deploy.zip --type zip

Write-Host "--------------------------------------------------" -ForegroundColor Green
Write-Host "デプロイ完了！"
Write-Host "URL: https://$appName.azurewebsites.net"
Write-Host "TURN サーバー: $turnUrl"
Write-Host "--------------------------------------------------"

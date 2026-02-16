# Azure リソース削除スクリプト

if (!(Get-Command az -ErrorAction SilentlyContinue)) {
    Write-Error "Azure CLI (az) が見つかりません。先に 'mise run setup' を実行してください。"
    exit 1
}

$resourceGroup = "rg-webrtc-poc"

Write-Host "リソースグループ '$resourceGroup' を削除しています... (非同期)" -ForegroundColor Cyan
Write-Host "削除が完了するまで数分かかる場合がありますが、コマンドはすぐに終了します。" -ForegroundColor Gray

# --no-wait で非同期削除、--yes で確認プロンプトスキップ
az group delete --name $resourceGroup --yes --no-wait

if ($?) {
    Write-Host "削除リクエストを送信しました。" -ForegroundColor Green
} else {
    Write-Error "削除リクエストの送信に失敗しました。"
    exit 1
}

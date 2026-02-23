# 指定時刻まで録画を繰り返すスクリプト

param (
    [string]$targetTime = ""
)

$ffmpeg = "bin\ffmpeg.exe"
$outputDir = "records"

# フォルダがない場合は作成
if (-not (Test-Path $outputDir)) {
    New-Item -ItemType Directory -Path $outputDir | Out-Null
}

if (-not (Test-Path $ffmpeg)) {
    Write-Error "FFmpeg not found in bin directory."
    exit 1
}

if ($targetTime) {
    Write-Host "Target time: $targetTime. Starting recording loop..."
} else {
    Write-Host "No target time specified. Starting infinite recording loop (Press Ctrl+C to stop)..."
}

while ($true) {
    # 終了時刻が指定されており、現在時刻がそれを過ぎていれば終了
    if ($targetTime -and (Get-Date) -ge (Get-Date $targetTime)) {
        Write-Host "Target time $targetTime reached. Stopping."
        break
    }

    # record.ps1 を呼び出し (30分=1800秒)
    # 戻り値 $LASTEXITCODE を確認
    & .\record.ps1 -t 1800
    
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "record.ps1 exited with error code $LASTEXITCODE"
        break
    }
}

Write-Host "Finished. Target time $targetTime reached."

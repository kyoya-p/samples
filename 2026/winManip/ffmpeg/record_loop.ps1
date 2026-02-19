# 指定時刻まで録画を繰り返すスクリプト
# 使い方: .ecord_loop.ps1 "17:00"

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

    $timestamp = Get-Date -Format "yyyyMMdd-HHmm"
    $outputFile = Join-Path $outputDir "output-$timestamp-b1k-t600-q45-p10.mp4"
    
    Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Recording to $outputFile ..."
    
    # 10分間 (600秒) 録画
    # -r 1/10: 10秒に1フレーム (極低フレームレート)
    # -b:v 1k: 極低ビットレート
    # -crf 45: 低画質圧縮
    & $ffmpeg -y -f gdigrab -i desktop -r 1/10 -c:v libx264 -b:v 1k -crf 45 -t 600 $outputFile
    
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "FFmpeg exited with error code $LASTEXITCODE"
        break
    }
}

Write-Host "Finished. Target time $targetTime reached."

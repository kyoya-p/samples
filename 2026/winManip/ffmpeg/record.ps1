# 単発録画スクリプト

param (
    [string]$t = "0", # デフォルト無制限
    [string]$r = "1/10", # デフォルト10秒に1フレーム
    [string]$outputName = ""
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

$timestamp = Get-Date -Format "yyyyMMdd-HHmm"
if (-not $outputName) {
    # ファイル名に使用できない文字を置換
    $safeR = $r -replace "/", "_"
    $outputFile = Join-Path $outputDir "output-$timestamp-r$safeR-t$t.mp4"
} else {
    $outputFile = Join-Path $outputDir $outputName
}

Write-Host "[$(Get-Date -Format 'HH:mm:ss')] Recording to $outputFile ..."

# 引数の組み立て
$args = @("-y", "-f", "gdigrab", "-i", "desktop", "-r", $r, "-c:v", "libx264", "-b:v", "1k", "-crf", "45")
if ($t -and $t -ne "0") {
    $args += @("-t", $t)
}
$args += $outputFile

& $ffmpeg $args

if ($LASTEXITCODE -ne 0) {
    Write-Warning "FFmpeg exited with error code $LASTEXITCODE"
    exit $LASTEXITCODE
}

Write-Host "Finished."

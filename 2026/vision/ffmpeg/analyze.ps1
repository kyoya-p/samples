# 動画ファイルをフレームごとにGeminiで解析しレポートを作成するスクリプト
# 使い方: .\analyze.ps1 "records/video.mp4"

param (
    [string]$videoPath = "",
    [string]$reportFile = "report.md",
    [string]$fps = "", # 空の場合は全フレーム抽出
    [switch]$h # ヘルプ表示
)

if ($h) {
    Write-Host "Usage: .\analyze.ps1 [-videoPath <path>] [-reportFile <path>] [-fps <rate>]"
    Write-Host ""
    Write-Host "Options:"
    Write-Host "  -videoPath  Path to the video file. If omitted, the latest .mp4 in records/ is used."
    Write-Host "  -reportFile Path to the output markdown report. Default: report.md"
    Write-Host "  -fps        Frame extraction rate (e.g., '1/10' for one frame every 10s). Default: extract all."
    Write-Host "  -h          Show this help message."
    exit 0
}

# videoPathが指定されていない場合は最新のファイルを探す
if (-not $videoPath) {
    $lastVideo = Get-ChildItem "records/*.mp4" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($lastVideo) {
        $videoPath = $lastVideo.FullName
        Write-Host "Auto-detected latest video: $videoPath" -ForegroundColor Gray
    } else {
        Write-Error "No video file found in records/."
        exit 1
    }
}

# 先頭の @ を削除し、相対パスでも見つかるように調整
$videoPath = $videoPath -replace '^@', ''
if (-not (Test-Path $videoPath)) {
    $altPath = Join-Path "records" (Split-Path $videoPath -Leaf)
    if (Test-Path $altPath) {
        $videoPath = $altPath
    } else {
        Write-Error "Video file not found: $videoPath"
        exit 1
    }
}

$ffmpeg = "bin\ffmpeg.exe"
$geminiCmd = "gemini"
if (-not (Get-Command $geminiCmd -ErrorAction SilentlyContinue)) {
    $geminiCmd = "gemini.cmd"
}

# 作業用ディレクトリの作成
$workDir = ".analyze_work"
if (Test-Path $workDir) { Remove-Item -Recurse -Force $workDir }
New-Item -ItemType Directory -Path $workDir | Out-Null

Write-Host "Extracting frames from $videoPath ..." -ForegroundColor Cyan
$vf = if ($fps) { @("-vf", "fps=$fps") } else { @() }
# -vsync 0 を追加して低FPS動画から確実にフレームを抽出
& $ffmpeg -y -i $videoPath $vf -vsync 0 -q:v 2 (Join-Path $workDir "frame_%04d.jpg") 2>$null

$frames = Get-ChildItem -Path $workDir -Filter "*.jpg" | Sort-Object Name
if ($frames.Count -eq 0) {
    Write-Warning "No frames extracted. The video might be too short or corrupted."
    exit 0
}
Write-Host "Found $($frames.Count) frames. Starting analysis..." -ForegroundColor Cyan

# レポートの初期化
$videoName = (Get-Item $videoPath).Name
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
"## Video Analysis Report: $videoName" | Out-File -FilePath $reportFile -Append
"Generated at: $timestamp" | Out-File -FilePath $reportFile -Append
"---" | Out-File -FilePath $reportFile -Append

$count = 1
foreach ($frame in $frames) {
    $framePath = $frame.FullName
    Write-Host "[$count / $($frames.Count)] Analyzing $($frame.Name)..."
    
    # フレームごとのプロンプト
    $prompt = "このスクリーンショットの内容を簡潔に説明してください。実行中のアプリケーション、作業内容、画面上の重要なテキストを抽出してください。"
    
    # Geminiで解析 (プロンプト文字列内に画像パスを含める)
    $result = & $geminiCmd -p "$prompt `"$framePath`""
    
    # レポートに追記
    $relativeFramePath = Join-Path $workDir $frame.Name
    "### Frame: $($frame.Name)" | Out-File -FilePath $reportFile -Append -Encoding utf8
    "![image]($relativeFramePath)" | Out-File -FilePath $reportFile -Append -Encoding utf8
    "" | Out-File -FilePath $reportFile -Append -Encoding utf8
    $result | Out-File -FilePath $reportFile -Append -Encoding utf8
    "" | Out-File -FilePath $reportFile -Append -Encoding utf8
    "---" | Out-File -FilePath $reportFile -Append -Encoding utf8
    
    $count++
}

Write-Host "Analysis completed. Report saved to $reportFile" -ForegroundColor Green

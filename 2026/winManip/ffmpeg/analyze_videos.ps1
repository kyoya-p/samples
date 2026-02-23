# 指定MP4ファイルをGeminiで解析するスクリプト
# 使い方: .\analyze_videos.ps1 "records" or .\analyze_videos.ps1 "records/video.mp4"

param (
    [string]$targetPath = "records"
)

# gemini/ffmpegコマンドの確認
$geminiCmd = "gemini.cmd"
if (-not (Get-Command $geminiCmd -ErrorAction SilentlyContinue)) {
    $geminiCmd = "gemini"
}
$ffmpeg = "bin\ffmpeg.exe"

$tempDir = ".analyze"
if (-not (Test-Path $tempDir)) {
    New-Item -ItemType Directory -Path $tempDir | Out-Null
}

if (-not (Test-Path $targetPath)) {
    Write-Error "Path $targetPath not found."
    exit 1
}

# ファイルかディレクトリかを判定してリスト化
if (Test-Path $targetPath -PathType Leaf) {
    $videos = @(Get-Item $targetPath)
} else {
    $videos = Get-ChildItem -Path $targetPath -Filter "*.mp4" | Sort-Object Name
}

Write-Host "Starting analysis for $($videos.Count) video(s)..."

foreach ($video in $videos) {
    $filePath = $video.FullName
    Write-Host "`n--- Analyzing: $($video.Name) ---" -ForegroundColor Cyan
    
    # 中間ファイル（静止画）の生成
    # 60秒ごとに1枚抽出
    $videoTempDir = Join-Path $tempDir $video.BaseName
    if (-not (Test-Path $videoTempDir)) { New-Item -ItemType Directory -Path $videoTempDir | Out-Null }
    
    Write-Host "Extracting frames to $videoTempDir ..."
    & $ffmpeg -y -i $filePath -vf "fps=1/60" -q:v 2 (Join-Path $videoTempDir "frame_%03d.jpg") 2>$null

    # プロンプトにビデオファイルと抽出した静止画ディレクトリを含める
    $prompt = "ファイル `"$filePath`" および `"$videoTempDir`" 内の画像を確認し、実施されている作業内容の詳細を時系列で列挙。画面上のテキストを可能な限り文字起こし"
    
    # 実行結果をファイルに出力
    $outputMd = "$filePath.md"
    Write-Host "Saving analysis results to $outputMd ..."
    & $geminiCmd -p "$prompt" -y > $outputMd
    
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Failed to analyze $($video.Name)"
    } else {
        Write-Host "Successfully analyzed and saved to $outputMd" -ForegroundColor Green
    }
}

Write-Host "`nAll analysis completed." -ForegroundColor Green

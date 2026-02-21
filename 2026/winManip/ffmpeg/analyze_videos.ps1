# 指定MP4ファイルをGeminiで解析するスクリプト
# 使い方: .\analyze_videos.ps1 "records" or .\analyze_videos.ps1 "records/video.mp4"

param (
    [string]$targetPath = "records"
)

# geminiコマンドの確認
$geminiCmd = "gemini.cmd"
if (-not (Get-Command $geminiCmd -ErrorAction SilentlyContinue)) {
    $geminiCmd = "gemini"
}

if (-not (Test-Path $targetPath)) {
    Write-Error "Path $targetPath not found."
    exit 1
}

# ファイルかディレクトリかを判定してリスト化
if (Test-Path $targetPath -PathType Leaf) {
    # 単一ファイルの場合も配列として扱う
    $videos = @(Get-Item $targetPath)
} else {
    $videos = Get-ChildItem -Path $targetPath -Filter "*.mp4" | Sort-Object Name
}

Write-Host "Starting analysis for $($videos.Count) video(s)..."

foreach ($video in $videos) {
    $filePath = $video.FullName
    Write-Host "`n--- Analyzing: $($video.Name) ---" -ForegroundColor Cyan
    
    # プロンプトに絶対パスを組み込む
    # 注意: gemini-cli はプロンプト内のパスを自動的にリソースとして認識する
    $prompt = "ファイル `"$filePath`" を確認し、実施されている作業内容の詳細を時系列で列挙。画面上のテキストを可能な限り文字起こし"
    
    # 実行
    & $geminiCmd -p "$prompt" -y
    
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Failed to analyze $($video.Name)"
    }
}

Write-Host "`nAll analysis completed." -ForegroundColor Green

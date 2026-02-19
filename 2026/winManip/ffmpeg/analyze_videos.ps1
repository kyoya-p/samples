# 指定フォルダ内のMP4ファイルをGeminiで解析するスクリプト
# 使い方: .\analyze_videos.ps1 "records"

param (
    [string]$targetDir = "records"
)

# geminiコマンドの確認 (.ps1が動かない環境を考慮して .cmd を優先)
$geminiCmd = "gemini.cmd"
if (-not (Get-Command $geminiCmd -ErrorAction SilentlyContinue)) {
    $geminiCmd = "gemini"
}

if (-not (Test-Path $targetDir)) {
    Write-Error "Directory $targetDir not found."
    exit 1
}

# MP4ファイルを列挙
$videos = Get-ChildItem -Path $targetDir -Filter "*.mp4" | Sort-Object Name

Write-Host "Found $($videos.Count) videos in $targetDir. Starting analysis..."

foreach ($video in $videos) {
    $filePath = $video.FullName
    Write-Host "`n--- Analyzing: $($video.Name) ---" -ForegroundColor Cyan
    
    # Gemini CLI を呼び出し
    # --yolo: ツール実行の確認をスキップ
    # プロンプトで詳細な解析と文字起こしを指示
    $prompt = "この動画ファイル ($($video.Name)) を確認し、実施されている作業内容の詳細を時系列で列挙。画面上のテキストを可能な限り文字起こし"
    
    # 実行
    # 注: gemini-cli が動画ファイルを直接解釈できるよう、コンテキストに含める必要がある場合があります
    & $geminiCmd -p "$prompt" -y
    
    if ($LASTEXITCODE -ne 0) {
        Write-Warning "Failed to analyze $($video.Name)"
    }
}

Write-Host "`nAll analysis completed." -ForegroundColor Green

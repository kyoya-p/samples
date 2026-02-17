param(
    [Parameter(Position=0)]
    [string]$Path = (Join-Path $PSScriptRoot "dependency-check")
)

$version = "12.1.0"
$zipFile = "dependency-check-$version-release.zip"
$url = "https://github.com/jeremylong/DependencyCheck/releases/download/v$version/$zipFile"

# 展開先を絶対パスに変換
$extractDir = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($Path)
$exePath = Join-Path $extractDir "bin\dependency-check.bat"

# exeが存在しない、または親ディレクトリが存在しない場合に展開を実行
if (-not (Test-Path $exePath)) {
    if (-not (Test-Path $zipFile)) {
        Write-Host "Downloading Dependency-Check v$version..."
        Invoke-WebRequest -Uri $url -OutFile $zipFile
    }

    # 一時フォルダに展開して移動、または直接展開
    # Zipは内部に "dependency-check" フォルダを持っているため、
    # $Path に直接展開すると $Path\dependency-check\... になる。
    # ここでは $Path が指す場所に bin フォルダが来るように調整する。
    
    $tempExtract = Join-Path $env:TEMP "dchk_temp_$(Get-Random)"
    New-Item -ItemType Directory -Path $tempExtract -Force | Out-Null
    
    Write-Host "Extracting $zipFile..."
    tar -xf $zipFile -C $tempExtract
    
    $extractedRoot = Join-Path $tempExtract "dependency-check"
    if (-not (Test-Path $extractDir)) {
        New-Item -ItemType Directory -Path $extractDir -Force | Out-Null
    }
    
    Write-Host "Installing to $extractDir..."
    Copy-Item -Path "$extractedRoot\*" -Destination $extractDir -Recurse -Force
    Remove-Item -Path $tempExtract -Recurse -Force
}

Write-Host "Verifying installation at $extractDir..."
if (Test-Path $exePath) {
    & $exePath --version
} else {
    Write-Error "Failed to find dependency-check.bat at $exePath"
    exit 1
}

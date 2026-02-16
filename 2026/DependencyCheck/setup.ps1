$version = "12.1.0"
$zipFile = "dependency-check-$version-release.zip"
$url = "https://github.com/jeremylong/DependencyCheck/releases/download/v$version/$zipFile"

# 環境変数 DEPENDENCY_CHECK_PATH を確認、なければデフォルト設定
$extractDir = $env:DEPENDENCY_CHECK_PATH
if (-not $extractDir) {
    $extractDir = Join-Path $PSScriptRoot "dependency-check"
}

if (-not (Test-Path $extractDir)) {
    if (-not (Test-Path $zipFile)) {
        Write-Host "Downloading Dependency-Check v$version..."
        Invoke-WebRequest -Uri $url -OutFile $zipFile
    }
    Write-Host "Extracting $zipFile to $extractDir..."
    tar -xf $zipFile
}

Write-Host "Verifying installation at $extractDir..."
$exePath = Join-Path $extractDir "bin\dependency-check.bat"
& $exePath --version

$ErrorActionPreference = "Stop"

# スクリプトのあるディレクトリ (tools/) に移動
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

Write-Host "Building executable JAR..."
.\amper.bat task :jvm-cli:executableJarJvm
if ($LASTEXITCODE -ne 0) {
    Write-Error "Build failed."
    exit $LASTEXITCODE
}

$installerDir = "build\installer"
$appName = "BS-CLI"
$appDir = Join-Path $installerDir $appName
$zipPath = Join-Path $installerDir "$appName.zip"
$inputJarDir = "build\tasks\_jvm-cli_executableJarJvm"
$mainJar = "jvm-cli-jvm-executable.jar"

Write-Host "Cleaning up previous installer build..."
if (Test-Path $appDir) { Remove-Item -Recurse -Force $appDir }
if (Test-Path $zipPath) { Remove-Item -Force $zipPath }

Write-Host "Creating application image using jpackage..."
# Note: --main-class is omitted to rely on the JAR's manifest
jpackage --type app-image `
    --input $inputJarDir `
    --dest $installerDir `
    --name $appName `
    --main-jar $mainJar `
    --java-options "-Dfile.encoding=UTF-8" `
    --win-console

if ($LASTEXITCODE -ne 0) {
    Write-Error "jpackage failed."
    exit $LASTEXITCODE
}

Write-Host "Compressing application to ZIP..."
Compress-Archive -Path $appDir -DestinationPath $zipPath

Write-Host "Package created successfully: $zipPath"

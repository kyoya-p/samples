# Firebase C++ SDK Setup Script for Windows

$SDK_VERSION = "13.3.0"
$SDK_FILENAME = "firebase_cpp_sdk_${SDK_VERSION}_windows.zip"
# GitHub Releases からのダウンロード URL
$SDK_URL = "https://github.com/firebase/firebase-cpp-sdk/releases/download/v${SDK_VERSION}/${SDK_FILENAME}"

$BUILD_DIR = Join-Path $PSScriptRoot "build"
$SDK_DIR = Join-Path $BUILD_DIR "sdk"
$ZIP_PATH = Join-Path $BUILD_DIR $SDK_FILENAME

# 1. ディレクトリ作成
if (-not (Test-Path $SDK_DIR)) {
    Write-Host "Creating directory: $SDK_DIR"
    New-Item -ItemType Directory -Force -Path $SDK_DIR | Out-Null
}

# 2. ダウンロード
Write-Host "Downloading Firebase C++ SDK v${SDK_VERSION}..."
Write-Host "URL: $SDK_URL"

try {
    Invoke-WebRequest -Uri $SDK_URL -OutFile $ZIP_PATH
}
catch {
    Write-Error "Download failed. Please check the version or internet connection."
    exit 1
}

# 3. 展開
Write-Host "Extracting to $SDK_DIR..."
# 展開先に既にファイルがある場合は上書きするか、一度クリアする処理を入れるのが一般的だが、
# ここではシンプルに展開する（Force）。
Expand-Archive -Path $ZIP_PATH -DestinationPath $SDK_DIR -Force

# 4. クリーンアップ (zip削除)
Write-Host "Cleaning up zip file..."
Remove-Item $ZIP_PATH

# 5. パスの確認と案内
# 展開されると $SDK_DIR/firebase_cpp_sdk みたいな階層になることが多い
$EXTRACTED_ROOT = Join-Path $SDK_DIR "firebase_cpp_sdk"

if (Test-Path $EXTRACTED_ROOT) {
    Write-Host "`n[SUCCESS] SDK setup complete."
    Write-Host "SDK Location: $EXTRACTED_ROOT"
    Write-Host "`nYou can now build the project using:"
    Write-Host "cmake -S . -B build -DFIREBASE_CPP_SDK_DIR=`"$EXTRACTED_ROOT`""
} else {
    Write-Warning "SDK seems to be extracted, but the expected folder '$EXTRACTED_ROOT' was not found."
    Write-Warning "Check inside '$SDK_DIR' to find the correct path."
}

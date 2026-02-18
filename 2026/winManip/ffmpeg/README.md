# 概要
FFmpeg 環境構築

プロジェクトローカルに `ffmpeg` をセットアップした手順。

# セットアップ手順

1.  **バイナリ取得**

```powershell
$url = "https://www.gyan.dev/ffmpeg/builds/ffmpeg-release-essentials.zip";
$zipPath = "ffmpeg.zip";
$extractPath = "ffmpeg_extracted";
$binPath = "bin";
Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing;
Add-Type -AssemblyName System.IO.Compression.FileSystem;
[System.IO.Compression.ZipFile]::ExtractToDirectory((Convert-Path $zipPath), (New-Item -ItemType Directory -Force -Path $extractPath).FullName);
New-Item -ItemType Directory -Force -Path $binPath | Out-Null;
Get-ChildItem -Path $extractPath -Recurse -Filter "ffmpeg.exe" | Move-Item -Destination "$binPath\ffmpeg.exe" -Force;
Get-ChildItem -Path $extractPath -Recurse -Filter "ffprobe.exe" | Move-Item -Destination "$binPath\ffprobe.exe" -Force;
Remove-Item -Path $extractPath -Recurse -Force; Remove-Item -Path $zipPath -Force;
```

2.  **実行ファイル配置**
    解凍したフォルダ内の `bin` にある `ffmpeg.exe` および `ffprobe.exe` を、本プロジェクトの `bin/` ディレクトリへコピー。

3.  **パスの設定 (`mise.toml`)**
    `mise` でパスを自動的に通すため、以下の設定を追加。
    ```toml
    [env]
    _.path = ["./bin"]
    ```

# 動作確認
```powershell: mise 経由での実行確認
mise exec -- ffmpeg -version
```
```powershell: 全画面動画キャプチャ
ffmpeg -f gdigrab -framerate 30 -i desktop -c:v libx264 -pix_fmt yuv420p -r 30 -q:v 3 output.mp4
bin\ffmpeg -f gdigrab -framerate 1 -i desktop -c:v libx264 -b:v 64k -t 10 output-64k10.mp4
bin\ffmpeg -f gdigrab -framerate 1 -i desktop -c:v libx264 -b:v 32k -t 10 output-32k10.mp4
bin\ffmpeg -f gdigrab -r 1/2 -i desktop -c:v libx264 -b:v 16k -t 10 output-16kp2.mp4
bin\ffmpeg -f gdigrab -r 1/4 -i desktop -c:v libx264 -b:v 16k -t 10 output-16kp4.mp4
bin\ffmpeg -f gdigrab -r 1/5 -i desktop -c:v libx264 -b:v 10k -t 10 output-10kp5.mp4
bin\ffmpeg -f gdigrab -r 1/10 -i desktop -c:v libx264 -b:v 1k -t 60 output-1k60p10.mp4
bin\ffmpeg -f gdigrab -crf 16 -i desktop -c:v libx264 -b:v 1k -t 60 output-1k60v16.mp4
```

# 情報ソース
- [gyan.dev FFmpeg Builds](https://www.gyan.dev/ffmpeg/builds/)
- [mise-en-place documentation](https://mise.jdx.dev/)

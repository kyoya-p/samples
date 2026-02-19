# 概要
FFmpeg 環境構築

プロジェクトローカルに `ffmpeg` をセットアップした手順。

# セットアップ

```powershell
$url = "https://github.com/BtbN/FFmpeg-Builds/releases/download/latest/ffmpeg-master-latest-win64-gpl.zip";
$zipPath = "ffmpeg.zip";
$extractPath = "ffmpeg_extracted";
$binPath = "bin";
Invoke-WebRequest -Uri $url -OutFile $zipPath -Proxy "$env:https_proxy" -ProxyUseDefaultCredentials -UseBasicParsing;
Add-Type -AssemblyName System.IO.Compression.FileSystem;
[System.IO.Compression.ZipFile]::ExtractToDirectory((Convert-Path $zipPath), (New-Item -ItemType Directory -Force -Path $extractPath).FullName);
New-Item -ItemType Directory -Force -Path $binPath | Out-Null;
Get-ChildItem -Path $extractPath -Recurse -Filter "ffmpeg.exe" | Move-Item -Destination "$binPath\ffmpeg.exe" -Force;
Get-ChildItem -Path $extractPath -Recurse -Filter "ffprobe.exe" | Move-Item -Destination "$binPath\ffprobe.exe" -Force;
Remove-Item -Path $extractPath -Recurse -Force; Remove-Item -Path $zipPath -Force;
```

```powershell: 全画面動画キャプチャ
ffmpeg -f gdigrab -framerate 30 -i desktop -c:v libx264 -pix_fmt yuv420p -r 30 -q:v 3 output.mp4
bin\ffmpeg -f gdigrab -framerate 1 -i desktop -c:v libx264 -b:v 64k -t 10 output-64k10.mp4
bin\ffmpeg -f gdigrab -framerate 1 -i desktop -c:v libx264 -b:v 32k -t 10 output-32k10.mp4
bin\ffmpeg -f gdigrab -r 1/2 -i desktop -c:v libx264 -b:v 16k -t 10 output-16kp2.mp4
bin\ffmpeg -f gdigrab -r 1/4 -i desktop -c:v libx264 -b:v 16k -t 10 output-16kp4.mp4
bin\ffmpeg -f gdigrab -r 1/5 -i desktop -c:v libx264 -b:v 10k -t 10 output-10kp5.mp4
bin\ffmpeg -f gdigrab -r 1/10 -i desktop -c:v libx264 -b:v 1k -t 60 output-1k60p10.mp4
bin\ffmpeg -f gdigrab -i desktop -c:v libx264 -crf 16 -t 60 output-1k60v16.mp4
bin\ffmpeg -y -f gdigrab -i desktop -c:v libx264 -b:v 64k -crf 8 -t 60 output-64k60q8.mp4
bin\ffmpeg -y -f gdigrab -i desktop -c:v libx264 -b:v 32k -crf 8 -t 60 output-32k60q8.mp4
bin\ffmpeg -y -f gdigrab -i desktop -c:v libx264 -b:v 16k -crf 8 -t 60 output-16k60q8.mp4
bin\ffmpeg -y -f gdigrab -i desktop -c:v libx264 -b:v 8k -crf 4 -t 60 output-8k60q4.mp4
```
```powershell: 全画面動画キャプチャ 監視カメラモード
bin\ffmpeg -y -f gdigrab -i desktop -r 1/30 -c:v libx264 -b:v 1k -crf 4 -t 3600 output-1k3600q4p30.mp4
bin\ffmpeg -y -f gdigrab -i desktop -r 1/30 -c:v libx264 -b:v 1k -t 3600 output-r1kt3600p30.mp4
bin\ffmpeg -y -f gdigrab -i desktop -c:v libx264 -b:v 1k -crf 45 -t 600 output-b1k-t600-q45.mp4
bin\ffmpeg -y -f gdigrab -i desktop -c:v libx264 -b:v 1k -crf 50 -t 600 output-b1k-t10m-q50.mp4
bin\ffmpeg -y -f gdigrab -i desktop -c:v libx264 -b:v 1k -crf 60 -t 600 output-b1k-t600-q60.mp4
bin\ffmpeg -y -f gdigrab -i desktop -r 1/10 -c:v libx264 -b:v 1k -crf 45 -t 600 output-b1k-t600-q45-p10.mp4
bin\ffmpeg -y -f gdigrab -i desktop -r 1/10 -c:v libx264 -b:v 1k -crf 45 -t 3600 output-b1k-t3600-q45-p10.mp4
```
# 情報ソース
- [gyan.dev FFmpeg Builds](https://www.gyan.dev/ffmpeg/builds/)
- [BtbN/FFmpeg-Builds (GitHub)](https://github.com/BtbN/FFmpeg-Builds)
- [mise-en-place documentation](https://mise.jdx.dev/)

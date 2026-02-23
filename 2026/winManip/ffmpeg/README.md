# 概要
画面録画とAI監視

# 環境
- Windows 11
- [mise](https://qiita.com/shokkaa/items/693260de34d2cb76767b)

# セットアップ

```powershell
mise run setup
```

# 録画起動
```powershell
mise run record  # Ctrl-Cで停止するまで 10秒ごとに1フレーム を録画
mise run record -- -t 10m -r "1/10" # 10分間, 10秒ごとに1フレームを録画
```

# 動画文書化
フレームごとに解析を実行
```powershell
# 18:00まで10分おきに録画を繰り返す
.\record_loop.ps1 "18:00"
```
# 情報ソース

- [gyan.dev FFmpeg Builds](https://www.gyan.dev/ffmpeg/builds/)
- [BtbN/FFmpeg-Builds (GitHub)](https://github.com/BtbN/FFmpeg-Builds)
- [mise-en-place documentation](https://mise.jdx.dev/)

# PowerShell Windowテストツール

## wincli.ps1

起動: `wincli.ps1 [-TargetPid <pid>] [-TargetHandle <hwnd>] [-h]`

標準入力からコマンド受信し、指定したプロセスまたはウィンドウハンドルに対し処理を実行します。

### 特徴
- **バックグラウンドキャプチャ**: `PrintWindow` (0x2) を使用しているため、ターゲットが他のウィンドウの下に隠れていても撮影可能です。
- **自動クロップ**: Windows Terminal の場合、UI Automation を使用してコンソール領域のみを自動的に切り抜きます。
- **親プロセス自動検索**: PID を指定した際、そのプロセスがウィンドウを持たない場合（コンソールアプリ等）は親プロセスを自動的に遡ってウィンドウを探します。

### 標準入力コマンド:
- `ss`: スクリーンショット採取
  - ファイル名は `.\.wincli\ss.<ID>.<yymmdd.hhmmss>.png`
- `cl x y`: マウスクリック (Window内相対座標)
- `k keys`: キー押下 (.NET SendKeys 準拠)
  - ※キー送信時は対象を前面に移動します。
- `exit`: 終了

### 実行例
```powershell
# 特定のハンドルを直接指定してバックグラウンドで撮影
"ss`nexit" | powershell -ExecutionPolicy Bypass -File .\wincli.ps1 -TargetHandle 39456362

# PIDを指定してキー送信後に撮影
"k ls{ENTER}`nss`nexit" | powershell -ExecutionPolicy Bypass -File .\wincli.ps1 -TargetPid 1168
```

## ウィンドウハンドルの詳細列挙

`Enumerate-AllHandles.ps1` を使用。
特定の PID に関連付けられたすべてのウィンドウハンドル（子ウィンドウ、不可視ウィンドウを含む）を列挙。

```powershell
# 特定の PID に関連する全ハンドルを表示
powershell -ExecutionPolicy Bypass -File .\Enumerate-AllHandles.ps1 -TargetPid 9680

# 可視 (Visible) なウィンドウのみを抽出するワンライナー
powershell -ExecutionPolicy Bypass -Command ".\Enumerate-AllHandles.ps1 | Where-Object { `$_.Visible } | Format-Table -AutoSize"
```

# Know How (PowerShell 知識ベース)

## Listen 状態のアプリ列挙
```powershell
Get-NetTCPConnection -State Listen | 
    Select-Object LocalAddress, LocalPort, OwningProcess, @{N='ProcessName';E={(Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue).ProcessName}} | 
    Sort-Object LocalPort | 
    Format-Table -AutoSize
```

## スクリーンショット (全画面)
```powershell
Add-Type -AssemblyName System.Windows.Forms,System.Drawing; $s=[System.Windows.Forms.Screen]::PrimaryScreen; $b=New-Object System.Drawing.Bitmap($s.Bounds.Width,$s.Bounds.Height); $g=[System.Drawing.Graphics]::FromImage($b); $g.CopyFromScreen($s.Bounds.Location,[System.Drawing.Point]::Empty,$s.Bounds.Size); $b.Save("screenshot.png",[System.Drawing.Imaging.ImageFormat]::Png); $g.Dispose(); $b.Dispose()
```
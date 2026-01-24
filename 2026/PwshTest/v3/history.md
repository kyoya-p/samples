# PowerShell知識ベース


## Listen 状態のアプリ列挙 (netstat 相当)

```powershell
Get-NetTCPConnection -State Listen | 
    Select-Object LocalAddress, LocalPort, OwningProcess, @{N='ProcessName';E={(Get-Process -Id $_.OwningProcess -ErrorAction SilentlyContinue).ProcessName}} | 
    Sort-Object LocalPort | 
    Format-Table -AutoSize
```

## スクリーンショットの取得 (全画面)

```powershell
Add-Type -AssemblyName System.Windows.Forms,System.Drawing; $s=[System.Windows.Forms.Screen]::PrimaryScreen; $b=New-Object System.Drawing.Bitmap($s.Bounds.Width,$s.Bounds.Height); $g=[System.Drawing.Graphics]::FromImage($b); $g.CopyFromScreen($s.Bounds.Location,[System.Drawing.Point]::Empty,$s.Bounds.Size); $b.Save("screenshot.png",[System.Drawing.Imaging.ImageFormat]::Png); $g.Dispose(); $b.Dispose()
```

## ウィンドウを持つプロセスの一覧表示

```powershell
Get-Process | Where-Object { $_.MainWindowTitle } | Select-Object @{N='ProcessName';E={$_.Name}}, @{N='ID';E={$_.Id}}, @{N='Title';E={if($_.MainWindowTitle.Length -gt 30){$_.MainWindowTitle.Substring(0,27) + "..."}else{$_.MainWindowTitle}}} | Sort-Object ProcessName | Format-Table -AutoSize
```

## 指定ウィンドウのスクリーンショット取得

`Get-WindowScreenshot.ps1` を使用。

```powershell
# プロセス名で指定
./Get-WindowScreenshot.ps1 -ProcessName notepad -FilePath notepad.png

# PID で指定
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./Get-WindowScreenshot.ps1 -ProcessId 13976 -FilePath "chrome_13976.png"
```

## ウィンドウハンドル (hWnd) の列挙

`Enumerate-AllHandles.ps1` を使用して、プロセスに紐付く全ハンドル（子ウィンドウ含む）を抽出。

```powershell
# 特定の PID に関連する全ハンドルを表示
./Enumerate-AllHandles.ps1 -TargetPid 1168

# 可視 (Visible) なウィンドウのみを抽出する
./Enumerate-AllHandles.ps1 | Where-Object { $_.Visible } | Format-Table -AutoSize
```

## プロセス所有権に基づくテキスト抽出 (UIA)

`Dump-TextByPid.ps1` を使用。シェルプロセスがウィンドウを直接持たない場合でも、親（Terminal）や子（conhost）を自動走査してテキストを抽出。

```powershell
# 指定した PID のコンソールテキストをダンプ
./Dump-TextByPid.ps1 -ProcessId 16784

# 見つかった全バッファ断片を .wincli\raw に保存
./Dump-TextByPid.ps1 -ProcessId 16784 -Save
```

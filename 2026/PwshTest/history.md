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
./Get-WindowScreenshot.ps1 -ProcessName notepad -FilePath notepad.png
```

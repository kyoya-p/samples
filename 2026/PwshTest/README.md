# PowerShell Windowテストツール

# wincli.ps1

起動: wincli.ps1 -TargetPid <process-id>

標準入力からコマンド受信しTargetPidで指定したWindowに対し処理を実行
- '-h': ヘルプ表示
- 'ss': スクリーンショット採取
  - ファイル名は `.\.wincli\ss.<TargetPid>.yymmdd.hhmmss.png`
- 'cl x y': マウスクリック
  - x,yはWindow内の相対座標(タスクバーを含めた左上隅を(0,0)とする)
- 'k key': キー押下
  - 記法は .NET SendKeys に準拠
  - '{ENTER}': Enterキー, '{TAB}': Tabキー, '^s': Ctrl+S, '%{F4}': Alt+F4
- 'exit': 終了

## 実行例

```powershell
# Hello入力してスクリーンショットを撮る
"k Hello{ENTER} `n ss" | powershell -ExecutionPolicy Bypass -File .\wincli.ps1 -TargetPid 1234
```

# Konw How

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

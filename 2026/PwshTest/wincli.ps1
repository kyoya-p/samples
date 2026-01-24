param(
    [Parameter(Mandatory=$true)]
    [int]$TargetPid
)

# Win32 API定義
$definition = @'
    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);

    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

    [DllImport("user32.dll")]
    public static extern IntPtr PostMessage(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);

    [StructLayout(LayoutKind.Sequential)]
    public struct RECT {
        public int Left; public int Top; public int Right; public int Bottom;
    }

    public static IntPtr MakeLParam(int x, int y) {
        return (IntPtr)((y << 16) | (x & 0xFFFF));
    }
'@

Add-Type -MemberDefinition $definition -Name Win32Utils -Namespace Native -ErrorAction SilentlyContinue
Add-Type -AssemblyName System.Windows.Forms, System.Drawing

$proc = Get-Process -Id $TargetPid -ErrorAction SilentlyContinue
if (-not $proc -or $proc.MainWindowHandle -eq 0) {
    Write-Error "Process with PID $TargetPid or its Window not found."
    exit 1
}

$hwnd = $proc.MainWindowHandle
Write-Host "Monitoring stdin for PID $TargetPid. Commands: ss (screenshot), cl x y (click), k key (keys), exit"

while ($true) {
    $line = [Console]::In.ReadLine()
    if ($null -eq $line) { break } # EOF (pipe closed)
    
    $line = $line.Trim()
    if ($line -eq "exit") { break }
    if ($line -eq "") { continue }

    $parts = $line.Split(' ', [StringSplitOptions]::RemoveEmptyEntries)
    if ($parts.Count -eq 0) { continue }

    switch ($parts[0]) {
        "ss" {
            # (既存の ss 処理)
            $date = Get-Date -Format "yyMMdd.HHmmss"
            $filename = "ss.$TargetPid.$date.png"
            
            [Native.Win32Utils]::ShowWindow($hwnd, 9) | Out-Null
            [Native.Win32Utils]::SetForegroundWindow($hwnd) | Out-Null
            Start-Sleep -Milliseconds 500

            $rect = [Native.Win32Utils+RECT]::new()
            [Native.Win32Utils]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
            $width = $rect.Right - $rect.Left
            $height = $rect.Bottom - $rect.Top
            
            Write-Host "Debug: Rect Left=$($rect.Left), Top=$($rect.Top), Right=$($rect.Right), Bottom=$($rect.Bottom)"
            Write-Host "Capturing window: ${width}x${height} at ($($rect.Left), $($rect.Top))"

            if ($width -gt 0 -and $height -gt 0) {
                $bitmap = New-Object System.Drawing.Bitmap($width, $height)
                $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
                $graphics.CopyFromScreen($rect.Left, $rect.Top, 0, 0, $bitmap.Size)
                $bitmap.Save($filename, [System.Drawing.Imaging.ImageFormat]::Png)
                $graphics.Dispose()
                $bitmap.Dispose()
                Write-Host "Screenshot saved: $(Resolve-Path $filename)"
            }
        }
        "cl" {
            # (既存の cl 処理)
            if ($parts.Count -ge 3) {
                $x = [int]$parts[1]
                $y = [int]$parts[2]
                $lParam = [Native.Win32Utils]::MakeLParam($x, $y)
                [Native.Win32Utils]::PostMessage($hwnd, 0x0201, [IntPtr]1, $lParam) | Out-Null # WM_LBUTTONDOWN
                Start-Sleep -Milliseconds 10
                [Native.Win32Utils]::PostMessage($hwnd, 0x0202, [IntPtr]0, $lParam) | Out-Null # WM_LBUTTONUP
                Write-Host "Clicked at ($x, $y)"
            }
        }
        { $_ -eq "k" -or $_ -eq "key" } {
            if ($parts.Count -ge 2) {
                $keys = $parts[1..($parts.Count-1)] -join " "
                [Native.Win32Utils]::SetForegroundWindow($hwnd) | Out-Null
                Start-Sleep -Milliseconds 100
                [System.Windows.Forms.SendKeys]::SendWait($keys)
                Write-Host "Sent keys: $keys"
            }
        }
        default {
            Write-Host "Unknown command: $line"
        }
    }
}

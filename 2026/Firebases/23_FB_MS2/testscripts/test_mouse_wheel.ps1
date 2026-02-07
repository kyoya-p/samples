# test_mouse_wheel.ps1
$signature = @"
using System;
using System.Runtime.InteropServices;
public class WinAPI {
    [DllImport("user32.dll")]
    public static extern bool SetCursorPos(int X, int Y);

    [DllImport("user32.dll")]
    public static extern void mouse_event(uint dwFlags, int dx, int dy, int dwData, int dwExtraInfo);

    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);

    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    [DllImport("user32.dll")]
    public static extern int GetWindowText(IntPtr hWnd, System.Text.StringBuilder lpString, int nMaxCount);

    [StructLayout(LayoutKind.Sequential)]
    public struct RECT {
        public int Left; public int Top; public int Right; public int Bottom;
    }

    public const uint MOUSEEVENTF_WHEEL = 0x0800;

    public static IntPtr FindWindowByTitle(string title) {
        IntPtr found = IntPtr.Zero;
        EnumWindows(delegate(IntPtr hWnd, IntPtr lParam) {
            System.Text.StringBuilder sb = new System.Text.StringBuilder(256);
            GetWindowText(hWnd, sb, 256);
            if (sb.ToString().Contains(title)) {
                found = hWnd;
                return false;
            }
            return true;
        }, IntPtr.Zero);
        return found;
    }
}
"@
try { Add-Type -TypeDefinition $signature -ErrorAction SilentlyContinue } catch {}

$uniqueTitle = "WheelTest_$(Get-Random)"
$appPath = Resolve-Path ".\build\Release\AddrApp.exe"
$env:API_KEY = "AIzaSyDpE5hkTVWMt8iYPPm30yNL6KJ-YivAwJ4"

Write-Host "Launching AddrApp for Mouse Wheel Test..."
Start-Process wt.exe -ArgumentList "-w new -d `"$((Get-Location).Path)`" --title $uniqueTitle `"$appPath`""
Start-Sleep -Seconds 10 

$hWnd = [WinAPI]::FindWindowByTitle($uniqueTitle)
if ($hWnd -ne [IntPtr]::Zero) {
    [WinAPI]::SetForegroundWindow($hWnd)
    Start-Sleep -Milliseconds 500

    $rect = New-Object WinAPI+RECT
    if ([WinAPI]::GetWindowRect($hWnd, [ref]$rect)) {
        $centerX = ($rect.Left + $rect.Right) / 2
        $centerY = ($rect.Top + $rect.Bottom) / 2
        
        Write-Host "Moving mouse to ($centerX, $centerY) and scrolling wheel..."
        [WinAPI]::SetCursorPos($centerX, $centerY)
        Start-Sleep -Milliseconds 200
        
        # 120 = 1 delta. Negative is down. Scroll down 5 times.
        for ($i=0; $i -lt 5; $i++) {
            [WinAPI]::mouse_event([WinAPI]::MOUSEEVENTF_WHEEL, 0, 0, -120, 0)
            Start-Sleep -Milliseconds 100
        }
        
        Write-Host "Capturing screen after wheel scroll..."
        powershell.exe -ExecutionPolicy Bypass -File sub_capture.ps1 -hWndStr "$hWnd" -filename "wheel_test_screenshot.png"
    }

    Write-Host "Closing application..."
    $wshell = New-Object -ComObject WScript.Shell
    if ($wshell.AppActivate($uniqueTitle)) {
        $wshell.SendKeys("q")
    }
}

Write-Host "Test Done. Check wheel_test_screenshot.png"

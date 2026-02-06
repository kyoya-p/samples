# run_app_with_screenshot.ps1
$signature = @"
using System;
using System.Runtime.InteropServices;
public class WinAPI {
    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    public static extern bool SetForegroundWindow(IntPtr hWnd);
    
    [DllImport("user32.dll")]
    public static extern bool IsIconic(IntPtr hWnd);
    
    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);
    public const int SW_RESTORE = 9;

    [DllImport("user32.dll")]
    public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    [DllImport("user32.dll")]
    public static extern int GetWindowText(IntPtr hWnd, System.Text.StringBuilder lpString, int nMaxCount);

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
Add-Type -TypeDefinition $signature
Add-Type -AssemblyName System.Windows.Forms, System.Drawing

$code = @"
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Windows.Forms;
public class FullScreenCapture {
    public static void Capture(string filename) {
        Rectangle bounds = Screen.PrimaryScreen.Bounds;
        using (Bitmap bmp = new Bitmap(bounds.Width, bounds.Height)) {
            using (Graphics g = Graphics.FromImage(bmp)) {
                g.CopyFromScreen(Point.Empty, Point.Empty, bounds.Size);
            }
            bmp.Save(filename, ImageFormat.Png);
        }
    }
}
"@
Add-Type -TypeDefinition $code -ReferencedAssemblies "System.Drawing", "System.Windows.Forms"

$uniqueTitle = "AddrApp_Run_$(Get-Random)"
$appPath = Resolve-Path ".\build\Release\AddrApp.exe"
$env:API_KEY = "AIzaSyDpE5hkTVWMt8iYPPm30yNL6KJ-YivAwJ4"

Write-Host "Launching AddrApp in Windows Terminal..."
Start-Process wt.exe -ArgumentList "-w new -d `"$((Get-Location).Path)`" --title $uniqueTitle `"$appPath`""
Start-Sleep -Seconds 10 # Wait for Firebase connection

$hWnd = [WinAPI]::FindWindowByTitle($uniqueTitle)
if ($hWnd -ne [IntPtr]::Zero) {
    [WinAPI]::SetForegroundWindow($hWnd)
    Start-Sleep -Milliseconds 500
    Write-Host "Capturing app screen..."
    [FullScreenCapture]::Capture("app_run_screenshot.png")
    
    # 追加：'q' を送って終了させる
    Write-Host "Closing application..."
    $wshell = New-Object -ComObject WScript.Shell
    if ($wshell.AppActivate($uniqueTitle)) {
        $wshell.SendKeys("q")
    }
}

Write-Host "App is running in a separate window. Screenshot saved to app_run_screenshot.png"

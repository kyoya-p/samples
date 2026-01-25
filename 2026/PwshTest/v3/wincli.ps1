param(
    [Parameter(Position=0)]
    [long]$Hwnd,
    [Parameter(Mandatory=$false)]
    [int]$TargetPid,
    [Parameter(Mandatory=$false)]
    [string]$OutputDir,
    [Switch]$h
)

if ($h) {
    Write-Host "Usage: wincli.ps1 [<hwnd>] [-TargetPid <pid>] [-OutputDir <path>] [-h]"
    Write-Host "  <hwnd>      Target Window Handle (Default: current console)"
    Write-Host "  -TargetPid <pid>  Target Process ID (Used to find hWnd if <hwnd> is missing)"
    Write-Host "  -OutputDir  Defaults to '.\.wincli'"
    Write-Host ""
    Write-Host "Standard Input Commands:"
    Write-Host "  ss          Take screenshot"
    Write-Host "  cl x y      Click at (x, y)"
    Write-Host "  k keys      Send keys (PostMessage, non-active window ok)"
    Write-Host "  dp          Dump text content (Last 100 lines)"
    Write-Host "  exit        Exit"
    exit 0
}

Add-Type -AssemblyName System.Windows.Forms, System.Drawing

# Win32 API定義
$source = @'
using System;
using System.Runtime.InteropServices;

namespace Native {
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT {
        public int Left; public int Top; public int Right; public int Bottom;
    }

    public class Win32Utils {
        [DllImport("kernel32.dll")]
        public static extern IntPtr GetConsoleWindow();

        [DllImport("user32.dll")]
        public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);

        [DllImport("user32.dll")]
        public static extern bool SetForegroundWindow(IntPtr hWnd);

        [DllImport("user32.dll")]
        public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

        [DllImport("user32.dll")]
        public static extern IntPtr PostMessage(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);

        [DllImport("user32.dll")]
        public static extern bool PrintWindow(IntPtr hWnd, IntPtr hdcBlt, uint nFlags);

        [DllImport("user32.dll")]
        public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);
        
        [DllImport("user32.dll")]
        public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

        [DllImport("user32.dll")]
        public static extern bool IsWindowVisible(IntPtr hWnd);

        [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        public static extern int GetClassName(IntPtr hWnd, System.Text.StringBuilder lpClassName, int nMaxCount);

        [DllImport("user32.dll", CharSet = CharSet.Auto, SetLastError = true)]
        public static extern int GetWindowText(IntPtr hWnd, System.Text.StringBuilder lpString, int nMaxCount);

        public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

        public static IntPtr MakeLParam(int x, int y) {
            return (IntPtr)((y << 16) | (x & 0xFFFF));
        }
    }
}
'@

Add-Type -TypeDefinition $source -ReferencedAssemblies System.Drawing, System.Windows.Forms -ErrorAction SilentlyContinue

function Get-WindowHandleForPid {
    param([int]$PidToFind)
    
    $script:foundHandle = [IntPtr]::Zero
    $script:fallbackHandle = [IntPtr]::Zero
    
    $proc = {
        param($hwnd, $lparam)
        $wPid = 0
        [Native.Win32Utils]::GetWindowThreadProcessId($hwnd, [ref]$wPid) | Out-Null
        
        if ($wPid -eq $PidToFind) {
            $rect = New-Object Native.RECT
            [Native.Win32Utils]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
            $width = $rect.Right - $rect.Left
            $height = $rect.Bottom - $rect.Top

            if ([Native.Win32Utils]::IsWindowVisible($hwnd) -and $width -gt 10 -and $height -gt 10) {
                $script:foundHandle = $hwnd
                return $false # Stop enumeration
            } else {
                if ($script:fallbackHandle -eq [IntPtr]::Zero) {
                    $script:fallbackHandle = $hwnd
                }
            }
        }
        return $true
    }
    
    $enumProc = [Native.Win32Utils+EnumWindowsProc]$proc
    try {
        [Native.Win32Utils]::EnumWindows($enumProc, [IntPtr]::Zero) | Out-Null
    } catch { }
    
    [GC]::KeepAlive($enumProc)
    
    if ($script:foundHandle -ne [IntPtr]::Zero) {
        return $script:foundHandle
    }
    return $script:fallbackHandle
}

$targetHwnd = [IntPtr]::Zero

if ($Hwnd -ne 0) {
    $targetHwnd = [IntPtr]$Hwnd
    Write-Host "Targeting Hwnd: $targetHwnd"
} elseif ($TargetPid -ne 0) {
    Write-Host "Resolving hWnd for PID: $TargetPid"
    $currentPid = $TargetPid
    $depth = 0
    $maxDepth = 10
    
    while ($depth -lt $maxDepth) {
        $proc = Get-Process -Id $currentPid -ErrorAction SilentlyContinue
        if ($proc) {
            if ($proc.MainWindowHandle -ne 0) {
                $targetHwnd = $proc.MainWindowHandle
                Write-Host "Found Window via MainWindowHandle: $targetHwnd ($($proc.ProcessName))"
                break
            }
            $enumHwnd = [IntPtr](Get-WindowHandleForPid $proc.Id)
            if ($enumHwnd -ne [IntPtr]::Zero) {
                $targetHwnd = $enumHwnd
                Write-Host "Found Window via EnumWindows: $targetHwnd ($($proc.ProcessName))"
                break
            }
        }
        
        # Parent lookup
        try {
            $cim = Get-CimInstance Win32_Process -Filter "ProcessId = $currentPid" -ErrorAction Stop
            if ($cim.ParentProcessId) {
                $currentPid = $cim.ParentProcessId
                $depth++
            } else { break }
        } catch { break }
    }
} else {
    $targetHwnd = [Native.Win32Utils]::GetConsoleWindow()
    Write-Host "No target specified. Using current console hWnd: $targetHwnd"
}

if ($targetHwnd -eq [IntPtr]::Zero) {
    Write-Error "Could not resolve a valid window handle."
    exit 1
}

$hwnd = $targetHwnd # Legacy alias for command blocks

Write-Host "Waiting 2 seconds for window state check..."
Start-Sleep -Seconds 2

# Try to find specific content area (TermControl) using UI Automation
$termControl = $null
try {
    Add-Type -AssemblyName UIAutomationClient, UIAutomationTypes -ErrorAction Stop
    $automationElement = [System.Windows.Automation.AutomationElement]::FromHandle($hwnd)
    
    $condition = New-Object System.Windows.Automation.PropertyCondition([System.Windows.Automation.AutomationElement]::ClassNameProperty, "TermControl")
    $termControl = $automationElement.FindFirst([System.Windows.Automation.TreeScope]::Descendants, $condition)

    if ($termControl) {
        Write-Host "UI Automation: Found 'TermControl' content area."
    }
} catch { }

if ([string]::IsNullOrEmpty($OutputDir)) {
    $OutputDir = Join-Path (Get-Location) ".wincli"
}
if (-not (Test-Path $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir | Out-Null }

Write-Host "Output directory: $OutputDir"
Write-Host "Monitoring stdin. Commands: ss, cl x y, k keys, dp, sleep ms, exit"

while ($true) {
    $line = [Console]::In.ReadLine()
    if ($null -eq $line) { break }
    
    $line = $line.Trim()
    if ($line -eq "exit") { break }
    if ($line -eq "") { continue }

    $parts = $line.Split(' ', [StringSplitOptions]::RemoveEmptyEntries)
    if ($parts.Count -eq 0) { continue }

    switch ($parts[0]) {
        "ss" {
            $date = Get-Date -Format "yyMMdd.HHmmss"
            $filename = Join-Path $outputDir "ss.h$($hwnd.ToInt64()).$date.png"
            
            $winRect = New-Object Native.RECT
            if ([Native.Win32Utils]::GetWindowRect($hwnd, [ref]$winRect)) {
                $winWidth = $winRect.Right - $winRect.Left
                $winHeight = $winRect.Bottom - $winRect.Top
                
                if ($winWidth -gt 0 -and $winHeight -gt 0) {
                    $bmp = New-Object System.Drawing.Bitmap($winWidth, $winHeight)
                    $graphics = [System.Drawing.Graphics]::FromImage($bmp)
                    $hdc = $graphics.GetHdc()
                    $success = [Native.Win32Utils]::PrintWindow($hwnd, $hdc, 0x2)
                    $graphics.ReleaseHdc($hdc)
                    $graphics.Dispose()

                    if ($success) {
                        if ($termControl) {
                            try {
                                $uiaRect = $termControl.Current.BoundingRectangle
                                $cropX = [int]$uiaRect.Left - $winRect.Left
                                $cropY = [int]$uiaRect.Top - $winRect.Top
                                $cropW = [int]$uiaRect.Width
                                $cropH = [int]$uiaRect.Height

                                if ($cropW -gt 0 -and $cropH -gt 0) {
                                    $cropRect = New-Object System.Drawing.Rectangle($cropX, $cropY, $cropW, $cropH)
                                    $croppedBmp = $bmp.Clone($cropRect, $bmp.PixelFormat)
                                    $bmp.Dispose()
                                    $bmp = $croppedBmp
                                }
                            } catch {}
                        }
                        $bmp.Save($filename, [System.Drawing.Imaging.ImageFormat]::Png)
                        Write-Host "Screenshot saved: $filename"
                    }
                    $bmp.Dispose()
                }
            }
        }
        "cl" {
            if ($parts.Count -ge 3) {
                $x = [int]$parts[1]
                $y = [int]$parts[2]
                $lParam = [Native.Win32Utils]::MakeLParam($x, $y)
                [Native.Win32Utils]::PostMessage($hwnd, 0x0201, [IntPtr]1, $lParam) | Out-Null
                Start-Sleep -Milliseconds 10
                [Native.Win32Utils]::PostMessage($hwnd, 0x0202, [IntPtr]0, $lParam) | Out-Null
                Write-Host "Clicked at ($x, $y)"
            }
        }
        { $_ -eq "k" -or $_ -eq "key" } {
            if ($parts.Count -ge 2) {
                $inputStr = $parts[1..($parts.Count-1)] -join " "
                $segments = $inputStr -split "(\{ENTER\})"
                foreach ($seg in $segments) {
                    if ($seg -eq "{ENTER}") {
                        [Native.Win32Utils]::PostMessage($hwnd, 0x0100, [IntPtr]0x0D, [IntPtr]::Zero) | Out-Null
                        [Native.Win32Utils]::PostMessage($hwnd, 0x0101, [IntPtr]0x0D, [IntPtr]::Zero) | Out-Null
                    } elseif ($seg.Length -gt 0) {
                        foreach ($char in $seg.ToCharArray()) {
                            [Native.Win32Utils]::PostMessage($hwnd, 0x0102, [IntPtr][int]$char, [IntPtr]::Zero) | Out-Null
                        }
                    }
                }
                Write-Host "Sent keys: $inputStr"
            }
        }
        "dp" {
            $text = $null
            try {
                $targetElement = if ($termControl) { $termControl } else { $automationElement }
                $patternObj = $null
                try {
                    $patternObj = $targetElement.GetCurrentPattern([System.Windows.Automation.TextPattern]::Pattern)
                } catch { }

                if (-not $patternObj) {
                    $allDescendants = $targetElement.FindAll([System.Windows.Automation.TreeScope]::Descendants, [System.Windows.Automation.Condition]::TrueCondition)
                    foreach ($desc in $allDescendants) {
                        try {
                            $patternObj = $desc.GetCurrentPattern([System.Windows.Automation.TextPattern]::Pattern)
                            if ($patternObj) { break }
                        } catch { }
                    }
                }

                if ($patternObj) {
                    $text = ([System.Windows.Automation.TextPattern]$patternObj).DocumentRange.GetText(-1)
                }
            } catch { }

            if (-not [string]::IsNullOrEmpty($text)) {
                $lines = $text.TrimEnd() -split "`n"
                if ($lines.Count -gt 100) { $lines = $lines[($lines.Count - 100)..($lines.Count - 1)] }
                $lines | ForEach-Object { Write-Host $_ }
            } else {
                Write-Error "No text found."
            }
        }
        "sleep" {
            if ($parts.Count -ge 2) { Start-Sleep -Milliseconds ([int]$parts[1]) }
        }
        { $_ -eq "-h" -or $_ -eq "help" } {
            Write-Host "Commands: ss, cl x y, k keys, dp, sleep ms, exit"
        }
        default {
            Write-Host "Unknown command: $line"
        }
    }
}
param(
    [Parameter(Mandatory=$false)]
    [int]$TargetPid,
    [Parameter(Mandatory=$false)]
    [long]$TargetHandle,
    [Switch]$h
)

if ($h -or ($null -eq $TargetPid -and $TargetHandle -eq 0 -and $args.Count -eq 0)) {
    Write-Host "Usage: wincli.ps1 [-TargetPid <pid>] [-TargetHandle <hwnd>] [-h]"
    Write-Host ""
    Write-Host "Standard Input Commands:"
    Write-Host "  ss          Take screenshot (using PrintWindow 0x2)"
    Write-Host "  cl x y      Click at (x, y)"
    Write-Host "  k keys      Send keys (alias: key)"
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

        public static IntPtr MakeLParam(int x, int y) {
            return (IntPtr)((y << 16) | (x & 0xFFFF));
        }
    }
}
'@

Add-Type -TypeDefinition $source -ReferencedAssemblies System.Drawing, System.Windows.Forms -ErrorAction SilentlyContinue

$hwnd = [IntPtr]::Zero

if ($TargetHandle -ne 0) {
    $hwnd = [IntPtr]$TargetHandle
    Write-Host "Targeting Window Handle: $hwnd"
} elseif ($TargetPid -ne 0) {
    $proc = Get-Process -Id $TargetPid -ErrorAction SilentlyContinue
    if (-not $proc) {
        Write-Error "Process with PID $TargetPid not found."
        exit 1
    }

    $currentProc = $proc
    $hwnd = $currentProc.MainWindowHandle
    $depth = 0
    $maxDepth = 5

    while (($hwnd -eq 0 -or $hwnd -eq [IntPtr]::Zero) -and $depth -lt $maxDepth) {
        try {
            $cimProc = Get-CimInstance Win32_Process -Filter "ProcessId = $($currentProc.Id)" -ErrorAction Stop
            $parentId = $cimProc.ParentProcessId
            if (-not $parentId) { break }
            
            $parentProc = Get-Process -Id $parentId -ErrorAction SilentlyContinue
            if (-not $parentProc) { break }

            Write-Host "PID $($currentProc.Id) ($($currentProc.Name)) has no window. Checking parent PID $($parentProc.Id) ($($parentProc.Name))..."
            $currentProc = $parentProc
            $hwnd = $currentProc.MainWindowHandle
            $depth++
        } catch {
            break
        }
    }
} else {
    Write-Error "Either -TargetPid or -TargetHandle is mandatory."
    exit 1
}

if ($hwnd -eq 0 -or $hwnd -eq [IntPtr]::Zero) {
    Write-Error "Valid window handle not found."
    exit 1
}

if ($TargetPid -ne 0) {
    Write-Host "Targeting Window of PID $($currentProc.Id) ($($currentProc.Name)) Handle: $hwnd"
}

# Try to find specific content area (TermControl) using UI Automation
$termControl = $null
try {
    Add-Type -AssemblyName UIAutomationClient, UIAutomationTypes -ErrorAction Stop
    $automationElement = [System.Windows.Automation.AutomationElement]::FromHandle($hwnd)
    
    # Search for TermControl (Windows Terminal content area)
    $condition = New-Object System.Windows.Automation.PropertyCondition([System.Windows.Automation.AutomationElement]::ClassNameProperty, "TermControl")
    $termControl = $automationElement.FindFirst([System.Windows.Automation.TreeScope]::Descendants, $condition)

    if ($termControl) {
        Write-Host "UI Automation: Found 'TermControl' content area. Screenshot will be cropped."
    }
} catch {
    # UIA failed or not available
}

$outputDir = Join-Path $PSScriptRoot ".wincli"
if (-not (Test-Path $outputDir)) { New-Item -ItemType Directory -Path $outputDir | Out-Null }

Write-Host "Monitoring stdin. Commands: ss (screenshot), cl x y (click), k key (keys), exit"

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
            $id = if ($TargetPid) { $TargetPid } else { $hwnd }
            $date = Get-Date -Format "yyMMdd.HHmmss"
            $filename = Join-Path $outputDir "ss.$id.$date.png"
            
            # Ensure window is visible/ready (optional but recommended)
            # [Native.Win32Utils]::ShowWindow($hwnd, 9) | Out-Null
            # [Native.Win32Utils]::SetForegroundWindow($hwnd) | Out-Null
            # Start-Sleep -Milliseconds 200

            # Get Window Rect (Screen Coordinates)

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

                                if ($cropW -gt 0 -and $cropH -gt 0 -and 
                                    $cropX -ge 0 -and $cropY -ge 0 -and 
                                    ($cropX + $cropW) -le $bmp.Width -and ($cropY + $cropH) -le $bmp.Height) {
                                    
                                    $cropRect = New-Object System.Drawing.Rectangle($cropX, $cropY, $cropW, $cropH)
                                    $croppedBmp = $bmp.Clone($cropRect, $bmp.PixelFormat)
                                    $bmp.Dispose()
                                    $bmp = $croppedBmp
                                    Write-Host "Cropped to content area ($cropW x $cropH)"
                                }
                            } catch {}
                        }

                        $bmp.Save($filename, [System.Drawing.Imaging.ImageFormat]::Png)
                        Write-Host "Screenshot saved: $filename"
                    } else {
                        Write-Error "PrintWindow failed."
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
                $keys = $parts[1..($parts.Count-1)] -join " "
                [Native.Win32Utils]::SetForegroundWindow($hwnd) | Out-Null
                Start-Sleep -Milliseconds 100
                [System.Windows.Forms.SendKeys]::SendWait($keys)
                Write-Host "Sent keys: $keys"
            }
        }
        { $_ -eq "-h" -or $_ -eq "help" } {
            Write-Host "Commands: ss, cl x y, k keys, exit"
        }
        default {
            Write-Host "Unknown command: $line"
        }
    }
}

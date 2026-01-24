param(
    [Parameter(Mandatory=$false)]
    [int]$TargetPid,
    [Parameter(Mandatory=$false)]
    [long]$TargetHandle,
    [Parameter(Mandatory=$false)]
    [string]$OutputDir,
    [Switch]$h
)

if ($h) {
    Write-Host "Usage: wincli.ps1 [-TargetPid <pid>] [-TargetHandle <hwnd>] [-OutputDir <path>] [-h]"
    Write-Host "  If no target is specified, a new 'pwsh' process is started."
    Write-Host "  -OutputDir defaults to '.\.wincli' in the current directory."
    Write-Host ""
    Write-Host "Standard Input Commands:"
    Write-Host "  ss          Take screenshot (using PrintWindow 0x2)"
    Write-Host "  cl x y      Click at (x, y)"
    Write-Host "  k keys      Send keys (alias: key)"
    Write-Host "  dump        Dump text content (via UI Automation)"
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

        [DllImport("user32.dll")]
        public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);
        
        [DllImport("user32.dll")]
        public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

        [DllImport("user32.dll")]
        public static extern bool IsWindowVisible(IntPtr hWnd);

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
            if ([Native.Win32Utils]::IsWindowVisible($hwnd)) {
                $script:foundHandle = $hwnd
                return $false # Stop enumeration (Found visible window)
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
    } catch {
        # Ignore errors during enumeration
    }
    
    # Garbage collect delegate to prevent premature cleanup
    [GC]::KeepAlive($enumProc)
    
    if ($script:foundHandle -ne [IntPtr]::Zero) {
        return $script:foundHandle
    }
    return $script:fallbackHandle
}

$hwnd = [IntPtr]::Zero
$autoSpawned = $false

if ($TargetHandle -eq 0 -and $TargetPid -eq 0) {
    $autoSpawned = $true
    Write-Host "No target specified. Starting new pwsh process..."
    try {
        # Use -NoExit to keep it open, and try to force a new window if possible
        $p = Start-Process pwsh -ArgumentList "-NoExit" -PassThru
    } catch {
        Write-Warning "'pwsh' not found. Trying 'powershell'..."
        $p = Start-Process powershell -ArgumentList "-NoExit" -PassThru
    }
    $TargetPid = $p.Id
    Write-Host "Started process PID: $TargetPid. Waiting for window..."
    
    # Wait for window handle to become available
    for ($i = 0; $i -lt 20; $i++) {
        $p.Refresh()
        # Try standard property first
        if ($p.MainWindowHandle -ne 0) { 
            $hwnd = $p.MainWindowHandle
            break 
        }
        
        # Try finding via EnumWindows
        $foundHwnd = [IntPtr](Get-WindowHandleForPid $TargetPid)
        if ($foundHwnd -ne [IntPtr]::Zero) {
            $hwnd = $foundHwnd
            break
        }
        
        # Check children (e.g. conhost)
        try {
            $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $($p.Id)" -ErrorAction SilentlyContinue
            foreach ($child in $children) {
                 $childProc = Get-Process -Id $child.ProcessId -ErrorAction SilentlyContinue
                 if ($childProc -and $childProc.MainWindowHandle -ne 0) {
                     $hwnd = $childProc.MainWindowHandle
                     Write-Host "Found window in child process: $($child.ProcessId) ($($child.Name))"
                     break
                 }
                 
                 $childHwnd = [IntPtr](Get-WindowHandleForPid $child.ProcessId)
                 if ($childHwnd -ne [IntPtr]::Zero) {
                     $hwnd = $childHwnd
                     Write-Host "Found window in child process via EnumWindows: $($child.ProcessId) ($($child.Name))"
                     break
                 }
            }
        } catch {}
        
        if ($hwnd -ne [IntPtr]::Zero) { break }

        Start-Sleep -Milliseconds 500
    }
}

if ($TargetHandle -ne 0) {
    $hwnd = [IntPtr]$TargetHandle
    Write-Host "Targeting Window Handle: $hwnd"
} elseif ($TargetPid -ne 0) {
    $currentPid = $TargetPid
    $depth = 0
    $maxDepth = 10
    $found = $false

    Write-Host "Searching for window handle starting from PID: $TargetPid"

    while ($depth -lt $maxDepth) {
        $proc = Get-Process -Id $currentPid -ErrorAction SilentlyContinue
        
        if ($proc) {
            # 1. Try standard MainWindowHandle
            if ($proc.MainWindowHandle -ne 0) {
                $hwnd = $proc.MainWindowHandle
                Write-Host "Found Window Owner via MainWindowHandle at depth $($depth): $($proc.ProcessName) (PID: $($proc.Id))"
                $currentProc = $proc
                $found = $true
                break
            }

            # 2. Try EnumWindows (for windows not reflected in MainWindowHandle)
            $enumHwnd = [IntPtr](Get-WindowHandleForPid $proc.Id)
            if ($enumHwnd -ne [IntPtr]::Zero) {
                $hwnd = $enumHwnd
                Write-Host "Found Window Owner via EnumWindows at depth $($depth): $($proc.ProcessName) (PID: $($proc.Id))"
                $currentProc = $proc
                $found = $true
                break
            }
        } else {
            Write-Warning "Process $currentPid no longer exists."
            break
        }

        # No window found, try parent
        try {
            $cimProc = Get-CimInstance Win32_Process -Filter "ProcessId = $currentPid" -ErrorAction Stop
            $parentId = $cimProc.ParentProcessId
            
            if (-not $parentId) { 
                Write-Warning "No parent process found for PID $currentPid."
                break 
            }
            
            # Write-Host "  -> Parent: $($cimProc.Name) (PID: $parentId)"
            $currentPid = $parentId
            $depth++
        } catch {
            break
        }
    }

    if (-not $found) {
        Write-Error "Could not find any window handle in the process tree of PID $TargetPid."
        exit 1
    }
}


Write-Host "Waiting 3 seconds for window initialization..."
Start-Sleep -Seconds 3

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

if ([string]::IsNullOrEmpty($OutputDir)) {
    $OutputDir = Join-Path (Get-Location) ".wincli"
}

if (-not (Test-Path $OutputDir)) { New-Item -ItemType Directory -Path $OutputDir | Out-Null }

Write-Host "Output directory: $OutputDir"
Write-Host "Monitoring stdin. Commands: ss (screenshot), cl x y (click), k key (keys), dump, exit"

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
        "dump" {
            $text = $null
            try {
                $targetElement = if ($termControl) { $termControl } else { $automationElement }
                $patternObj = $null
                
                # Try getting pattern directly
                try {
                    $patternObj = $targetElement.GetCurrentPattern([System.Windows.Automation.TextPattern]::Pattern)
                } catch { }

                # If failed, search for Document or Edit control types, or ANY element with TextPattern
                if (-not $patternObj) {
                    $allDescendants = $targetElement.FindAll([System.Windows.Automation.TreeScope]::Descendants, [System.Windows.Automation.Condition]::TrueCondition)
                    foreach ($desc in $allDescendants) {
                        try {
                            $patternObj = $desc.GetCurrentPattern([System.Windows.Automation.TextPattern]::Pattern)
                            if ($patternObj) {
                                Write-Host "Found element with TextPattern: '$($desc.Current.Name)' Class: $($desc.Current.ClassName)"
                                break
                            }
                        } catch { }
                    }
                }

                if ($patternObj) {
                    $textPattern = [System.Windows.Automation.TextPattern]$patternObj
                    $text = $textPattern.DocumentRange.GetText(-1)
                }
            } catch {
                Write-Warning "Text extraction failed or not supported: $_"
            }

            if (-not [string]::IsNullOrEmpty($text)) {
                $id = if ($TargetPid) { $TargetPid } else { $hwnd }
                $date = Get-Date -Format "yyMMdd.HHmmss"
                $dumpFile = Join-Path $outputDir "dump.$id.$date.txt"
                $text | Set-Content -Path $dumpFile -Encoding UTF8
                Write-Host "Text dumped: $dumpFile"
                $text -split "`n" | Select-Object -Last 5
            } else {
                Write-Error "No text found (TextPattern might not be supported)."
            }
        }
        { $_ -eq "-h" -or $_ -eq "help" } {
            Write-Host "Commands: ss, cl x y, k keys, dump, exit"
        }
        default {
            Write-Host "Unknown command: $line"
        }
    }
}
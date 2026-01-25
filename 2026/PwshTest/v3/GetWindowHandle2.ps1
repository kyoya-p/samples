param(
    [Parameter(Mandatory=$true)]
    [int]$ProcessId,
    [int]$MaxDepth = 5
)

# Load UIA Assemblies
Add-Type -AssemblyName UIAutomationClient, UIAutomationTypes

# Win32 API Definition
$source = @'
using System;
using System.Runtime.InteropServices;
using System.Text;
using System.Collections.Generic;

public class Win32 {
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    [DllImport("user32.dll")]
    public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);

    [DllImport("user32.dll")]
    public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

    [DllImport("user32.dll")]
    public static extern bool IsWindowVisible(IntPtr hWnd);

    [DllImport("user32.dll", CharSet = CharSet.Auto)]
    public static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);

    [DllImport("user32.dll", CharSet = CharSet.Auto)]
    public static extern int GetClassName(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);

    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);

    [StructLayout(LayoutKind.Sequential)]
    public struct RECT {
        public int Left; public int Top; public int Right; public int Bottom;
    }
}
'@

Add-Type -TypeDefinition $source -ErrorAction SilentlyContinue

function Get-VisibleWindowsForPid {
    param([int]$PidToFind)

    $windows = New-Object System.Collections.Generic.List[PSObject]

    $proc = [Win32+EnumWindowsProc] {
        param($hwnd, $lparam)
        $wPid = 0
        [Win32]::GetWindowThreadProcessId($hwnd, [ref]$wPid) | Out-Null

        if ($wPid -eq $PidToFind) {
            if ([Win32]::IsWindowVisible($hwnd)) {
                $sbTitle = New-Object System.Text.StringBuilder 256
                [Win32]::GetWindowText($hwnd, $sbTitle, $sbTitle.Capacity) | Out-Null
                
                $sbClass = New-Object System.Text.StringBuilder 256
                [Win32]::GetClassName($hwnd, $sbClass, $sbClass.Capacity) | Out-Null

                $rect = New-Object Win32+RECT
                [Win32]::GetWindowRect($hwnd, [ref]$rect) | Out-Null
                $width = $rect.Right - $rect.Left
                $height = $rect.Bottom - $rect.Top

                if ($width -gt 0 -and $height -gt 0) {
                    $windows.Add([PSCustomObject]@{
                        Handle = $hwnd
                        Title = $sbTitle.ToString()
                        Class = $sbClass.ToString()
                        Width = $width
                        Height = $height
                    })
                }
            }
        }
        return $true
    }

    [Win32]::EnumWindows($proc, [IntPtr]::Zero) | Out-Null
    return $windows
}

function Get-UiaTextContent {
    param([IntPtr]$Hwnd)
    
    $content = @()
    try {
        $el = [System.Windows.Automation.AutomationElement]::FromHandle($Hwnd)
        if (-not $el) { return @() }
        
        # Search for elements with TextPattern in the subtree
        $cond = [System.Windows.Automation.Condition]::TrueCondition
        $elements = $el.FindAll([System.Windows.Automation.TreeScope]::Subtree, $cond)
        
        foreach ($item in $elements) {
            try {
                $pattern = $item.GetCurrentPattern([System.Windows.Automation.TextPattern]::Pattern)
                if ($pattern) {
                    # Get full text
                    $txt = $pattern.DocumentRange.GetText(-1)
                    
                    if (-not [string]::IsNullOrWhiteSpace($txt)) {
                        $content += [PSCustomObject]@{
                            ElementPid = $item.Current.ProcessId
                            ElementName = $item.Current.Name
                            ElementClass = $item.Current.ClassName
                            Text = $txt
                        }
                    }
                }
            } catch {}
        }
    } catch {}
    
    return $content
}

# Main Search Loop
$currentPid = $ProcessId
$found = $false
$allResults = New-Object System.Collections.Generic.List[PSObject]

Write-Host "Searching window for PID $ProcessId (MaxDepth: $MaxDepth)..." -ForegroundColor Cyan

# 0. Show Process Tree
Write-Host "`nProcess Tree for PID ${ProcessId}:" -ForegroundColor Gray
$pTree = $ProcessId
$indent = ""
for ($i = 0; $i -le $MaxDepth; $i++) {
    $p = Get-Process -Id $pTree -ErrorAction SilentlyContinue
    if ($p) {
        Write-Host "$indent|- $($p.ProcessName) (PID: $($p.Id))" -ForegroundColor Gray
    } else {
        Write-Host "$indent|- (Process $pTree not found or exited)" -ForegroundColor DarkGray
        break
    }
    
    try {
        $cim = Get-CimInstance Win32_Process -Filter "ProcessId = $pTree" -ErrorAction Stop
        if ($cim.ParentProcessId -and $cim.ParentProcessId -ne 0) {
            $pTree = $cim.ParentProcessId
            $indent += "  "
        } else {
            break
        }
    } catch { break }
}
Write-Host ""

for ($i = 0; $i -le $MaxDepth; $i++) {
    $procInfo = Get-Process -Id $currentPid -ErrorAction SilentlyContinue
    if (-not $procInfo) {
        Write-Warning "Process $currentPid not found."
        break
    }

    Write-Host "Checking PID $currentPid ($($procInfo.ProcessName))..." -NoNewline

    # 1. Check direct windows via EnumWindows
    $windows = Get-VisibleWindowsForPid $currentPid
    
    if ($windows.Count -gt 0) {
        Write-Host " Found $($windows.Count) visible window(s)." -ForegroundColor Green
        foreach ($w in $windows) {
            $texts = Get-UiaTextContent $w.Handle
            
            if ($texts.Count -gt 0) {
                foreach ($t in $texts) {
                    $cleanText = $t.Text.TrimEnd().Replace("`r", "").Replace("`n", " ").Replace("`t", " ")
                    $snippet = if($cleanText.Length -gt 40) { "..." + $cleanText.Substring($cleanText.Length - 40) } else { $cleanText }
                    
                    $allResults.Add([PSCustomObject]@{
                        TargetPid    = $ProcessId
                        FoundPid     = $currentPid
                        ProcessName  = $procInfo.ProcessName
                        Handle       = $w.Handle
                        WindowTitle  = $w.Title
                        WindowClass  = $w.Class
                        TextPid      = $t.ElementPid
                        TextClass    = $t.ElementClass
                        TextSnippet  = $snippet
                        FullTextLength = $t.Text.Length
                        FullText     = $t.Text
                    })
                    $found = $true
                }
            } else {
                # ウィンドウはあるがテキストが取れない場合
                $allResults.Add([PSCustomObject]@{
                    TargetPid    = $ProcessId
                    FoundPid     = $currentPid
                    ProcessName  = $procInfo.ProcessName
                    Handle       = $w.Handle
                    WindowTitle  = $w.Title
                    WindowClass  = $w.Class
                    TextPid      = $null
                    TextClass    = $null
                    TextSnippet  = "(No TextPattern found)"
                    FullTextLength = 0
                })
                $found = $true
            }
        }
    } else {
        Write-Host " No visible windows."
    }

    # 親プロセスへ移動
    try {
        $cim = Get-CimInstance Win32_Process -Filter "ProcessId = $currentPid" -ErrorAction Stop
        if ($cim.ParentProcessId -and $cim.ParentProcessId -ne 0) {
            $currentPid = $cim.ParentProcessId
        } else {
            break
        }
    } catch {
        break
    }
}

Write-Host ""
if ($allResults.Count -gt 0) {
    # Group by FoundPid (Process) to show tree-like structure based on process hierarchy
    $grouped = $allResults | Group-Object FoundPid
    
    foreach ($group in $grouped) {
        $procName = $group.Group[0].ProcessName
        $foundPidVal = $group.Name
        Write-Host "Process: $procName (PID: $foundPidVal)" -ForegroundColor Cyan
        
        # Further group by Window Handle within the process
        $winGroups = $group.Group | Group-Object Handle
        foreach ($winGroup in $winGroups) {
            $first = $winGroup.Group[0]
            Write-Host "  └─ Window: [$($first.Handle)] '$($first.WindowTitle)' ($($first.WindowClass))" -ForegroundColor White
            
            foreach ($item in $winGroup.Group) {
                $snippet = $item.TextSnippet
                if ($snippet -ne "(No TextPattern found)") {
                    Write-Host "       └─ Text [$($item.TextClass)] (Size: $($item.FullTextLength)): $snippet" -ForegroundColor Gray
                }
            }
        }
        Write-Host ""
    }
} else {
    Write-Warning "No visible windows found in the process tree."
}

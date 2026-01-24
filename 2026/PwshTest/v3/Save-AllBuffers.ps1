param([int]$ProcessId)
$outDir = ".wincli\raw"
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Path $outDir | Out-Null }

Add-Type -AssemblyName UIAutomationClient, UIAutomationTypes
$source = @"
using System;
using System.Runtime.InteropServices;
public class Win32Utils_Raw {
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);
}
"@
Add-Type -TypeDefinition $source -ErrorAction SilentlyContinue

$relatedPids = @($ProcessId)
try { $parent = (Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId").ParentProcessId; if ($parent) { $relatedPids += $parent } } catch {}
Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" | ForEach-Object { $relatedPids += $_.ProcessId }

$handles = New-Object System.Collections.Generic.List[IntPtr]
$enumProc = [Win32Utils_Raw+EnumWindowsProc]{
    param($hwnd, $lparam)
    $wPid = 0; [Win32Utils_Raw]::GetWindowThreadProcessId($hwnd, [ref]$wPid) | Out-Null
    if ($script:relatedPids -contains $wPid) { $script:handles.Add($hwnd) }
    return $true
}
[Win32Utils_Raw]::EnumWindows($enumProc, [IntPtr]::Zero) | Out-Null

$count = 0
foreach ($hwnd in $handles) {
    try {
        $el = [System.Windows.Automation.AutomationElement]::FromHandle($hwnd)
        $elements = $el.FindAll([System.Windows.Automation.TreeScope]::Subtree, [System.Windows.Automation.Condition]::TrueCondition)
        foreach ($item in $elements) {
            try {
                $pattern = $item.GetCurrentPattern([System.Windows.Automation.TextPattern]::Pattern)
                if ($pattern) {
                    $text = $pattern.DocumentRange.GetText(-1)
                    if ($text -and $text.Trim().Length -gt 0) {
                        $count++
                        $filename = Join-Path $outDir ("buffer_pid_{0}_hwnd_{1}_idx_{2}.txt" -f $ProcessId, $hwnd.ToInt64(), $count)
                        $text | Set-Content -Path $filename -Encoding UTF8
                        Write-Host "Saved: $filename"
                    }
                }
            } catch {}
        }
    } catch {}
}

param (
    [Parameter(Mandatory=$true)]
    [int]$ProcessId,
    [Switch]$All,      # 見つかった全てのバッファを表示（デバッグ用）
    [Switch]$Save,     # .wincli\raw フォルダに全バッファを保存
    [string]$OutFile   # 指定したファイルに「最高の1つ」を保存
)

$ErrorActionPreference = "SilentlyContinue"
Add-Type -AssemblyName UIAutomationClient, UIAutomationTypes

# Win32 API for window enumeration
$source = @'
using System;
using System.Runtime.InteropServices;
using System.Collections.Generic;

public class Win32Utils_v2 {
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);
    [DllImport("user32.dll")] public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);
    [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);
    [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr hWnd);
}
'@
Add-Type -TypeDefinition $source -ErrorAction SilentlyContinue

# 1. 関連PID（自分、親、子）を収集
$relatedPids = @([int]$ProcessId)
$curr = $ProcessId
for ($i=0; $i -lt 5; $i++) {
    try {
        $cim = Get-CimInstance Win32_Process -Filter "ProcessId = $curr"
        if ($cim.ParentProcessId) { 
            $relatedPids += [int]$cim.ParentProcessId
            $curr = $cim.ParentProcessId 
        } else { break }
    } catch { break }
}
Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" | ForEach-Object { $relatedPids += [int]$_.ProcessId }
$relatedPids = $relatedPids | Select-Object -Unique

Write-Host "Target PIDs: $($relatedPids -join ', ')" -ForegroundColor Gray

# 2. ウィンドウハンドルを列挙
$foundWindows = New-Object System.Collections.Generic.List[PSObject]
$enumProc = [Win32Utils_v2+EnumWindowsProc]{
    param($hwnd, $lparam)
    $wPid = 0
    [Win32Utils_v2]::GetWindowThreadProcessId($hwnd, [ref]$wPid) | Out-Null
    if ($script:relatedPids -contains [int]$wPid) {
        $script:foundWindows.Add([PSCustomObject]@{ HWND = $hwnd; PID = [int]$wPid })
    }
    return $true
}
[Win32Utils_v2]::EnumWindows($enumProc, [IntPtr]::Zero) | Out-Null

foreach ($win in $foundWindows) {
    Write-Host "Found: hWnd=$($win.HWND), PID=$($win.PID)" -ForegroundColor Gray
}

# 3. テキスト抽出
$extracted = New-Object System.Collections.Generic.List[string]
$seenText = New-Object System.Collections.Generic.HashSet[string]

foreach ($win in $foundWindows) {
    try {
        $el = [System.Windows.Automation.AutomationElement]::FromHandle($win.HWND)
        $elements = $el.FindAll([System.Windows.Automation.TreeScope]::Subtree, [System.Windows.Automation.Condition]::TrueCondition)
        
        foreach ($item in $elements) {
            try {
                if ($relatedPids -notcontains [int]$item.Current.ProcessId) { continue }
                $pattern = $item.GetCurrentPattern([System.Windows.Automation.TextPattern]::Pattern)
                if (-not $pattern) { continue }
                
                $text = $pattern.DocumentRange.GetText(-1)
                if ([string]::IsNullOrWhiteSpace($text)) { continue }
                
                $trimmed = $text.Trim()
                if ($trimmed.Length -lt 5 -or $seenText.Contains($trimmed)) { continue }
                $seenText.Add($trimmed) | Out-Null
                $extracted.Add($trimmed)
            } catch { } 
        }
    } catch { } 
}

# 4. 結果の出力
if ($Save) {
    $rawDir = ".wincli\raw"
    if (-not (Test-Path $rawDir)) { New-Item -ItemType Directory -Path $rawDir | Out-Null }
    $idx = 0
    foreach ($txt in $extracted) {
        $idx++
        $path = Join-Path $rawDir ("buffer_i{0}.txt" -f $idx)
        $txt | Set-Content -Path $path -Encoding UTF8
    }
}

if ($OutFile) {
    ($extracted -join "`n`n--- Next Fragment ---`n`n") | Set-Content -Path $OutFile -Encoding UTF8
}

if ($extracted.Count -gt 0) {
    foreach ($txt in $extracted) {
        $txt
    }
} else {
    Write-Warning "No text content found for PID $ProcessId."
}

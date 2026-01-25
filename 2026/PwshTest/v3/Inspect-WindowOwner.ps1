param(
    [Parameter(Mandatory=$true)]
    [long]$Hwnd
)

# --- 1. Win32 API Definition ---
$source = @'
using System;
using System.Runtime.InteropServices;
using System.Text;
using System.Collections.Generic;

public class Win32Inspector {
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    [DllImport("user32.dll")]
    public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

    [DllImport("user32.dll")]
    public static extern bool EnumWindows(EnumWindowsProc lpEnumFunc, IntPtr lParam);

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

# --- 2. Get Owner PID from hWnd ---
$targetHwnd = [IntPtr]$Hwnd
$ownerPid = 0
[Win32Inspector]::GetWindowThreadProcessId($targetHwnd, [ref]$ownerPid) | Out-Null

Write-Host "`n=== Window Inspection: $Hwnd ===" -ForegroundColor Cyan

if ($ownerPid -eq 0) {
    Write-Error "Invalid Window Handle or Access Denied."
    exit 1
}

# --- 3. Process Information ---
$proc = Get-Process -Id $ownerPid -ErrorAction SilentlyContinue
if ($proc) {
    Write-Host "Owner Process: $($proc.ProcessName) (PID: $ownerPid)" -ForegroundColor Green
    Write-Host "  Path:        $($proc.Path)"
    Write-Host "  Start Time:  $($proc.StartTime)"
} else {
    Write-Host "Owner Process: (PID: $ownerPid) - Process may have exited or access denied." -ForegroundColor Yellow
}

# --- 4. Parent Process ---
try {
    $cim = Get-CimInstance Win32_Process -Filter "ProcessId = $ownerPid" -ErrorAction Stop
    if ($cim.ParentProcessId) {
        $parent = Get-Process -Id $cim.ParentProcessId -ErrorAction SilentlyContinue
        $pName = if ($parent) { $parent.ProcessName } else { "Unknown" }
        Write-Host "Parent Process: $pName (PID: $($cim.ParentProcessId))" -ForegroundColor Gray
    }
} catch {
    Write-Host "Parent Process: (Could not determine)" -ForegroundColor DarkGray
}

# --- 5. Sibling Windows (Owned by same PID) ---
Write-Host "`nAll Windows Owned by PID ${ownerPid}:" -ForegroundColor Cyan

$windows = New-Object System.Collections.Generic.List[PSObject]
$procDelegate = [Win32Inspector+EnumWindowsProc] {
    param($wHwnd, $lparam)
    $wPid = 0
    [Win32Inspector]::GetWindowThreadProcessId($wHwnd, [ref]$wPid) | Out-Null
    
    if ($wPid -eq $ownerPid) {
        $sbTitle = New-Object System.Text.StringBuilder 256
        [Win32Inspector]::GetWindowText($wHwnd, $sbTitle, $sbTitle.Capacity) | Out-Null
        
        $sbClass = New-Object System.Text.StringBuilder 256
        [Win32Inspector]::GetClassName($wHwnd, $sbClass, $sbClass.Capacity) | Out-Null

        $rect = New-Object Win32Inspector+RECT
        [Win32Inspector]::GetWindowRect($wHwnd, [ref]$rect) | Out-Null
        
        $isVisible = [Win32Inspector]::IsWindowVisible($wHwnd)
        
        $windows.Add([PSCustomObject]@{
            Handle  = $wHwnd
            Title   = $sbTitle.ToString()
            Class   = $sbClass.ToString()
            Size    = "$($rect.Right - $rect.Left)x$($rect.Bottom - $rect.Top)"
            Visible = $isVisible
            IsTarget = ($wHwnd -eq $targetHwnd)
        })
    }
    return $true
}

[Win32Inspector]::EnumWindows($procDelegate, [IntPtr]::Zero) | Out-Null

if ($windows.Count -gt 0) {
    $windows | Format-Table -AutoSize | Out-String | Write-Host
} else {
    Write-Warning "No windows found for this PID (Strange, since we started from a handle)."
}

# --- 6. Child Processes ---
Write-Host "Child Processes:" -ForegroundColor Cyan
try {
    $children = Get-CimInstance Win32_Process -Filter "ParentProcessId = $ownerPid" -ErrorAction SilentlyContinue
    if ($children) {
        foreach ($child in $children) {
            Write-Host "  |- $($child.Name) (PID: $($child.ProcessId))"
        }
    } else {
        Write-Host "  (None)"
    }
} catch {
    Write-Host "  (Error listing children)"
}
Write-Host ""

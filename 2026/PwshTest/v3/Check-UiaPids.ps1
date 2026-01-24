param([int]$ProcessId)
$ErrorActionPreference = "SilentlyContinue"
Add-Type -AssemblyName UIAutomationClient, UIAutomationTypes

$signature = @'
[DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);
[DllImport("user32.dll")] public static extern bool EnumWindows(EnumProc lpEnumFunc, IntPtr lParam);
public delegate bool EnumProc(IntPtr hWnd, IntPtr lParam);
'@
Add-Type -MemberDefinition $signature -Name Win32Utils_Debug -Namespace Win32

$relatedPids = New-Object System.Collections.Generic.HashSet[int]
$relatedPids.Add($ProcessId) | Out-Null
try { $relatedPids.Add((Get-CimInstance Win32_Process -Filter "ProcessId = $ProcessId").ParentProcessId) | Out-Null } catch {}
Get-CimInstance Win32_Process -Filter "ParentProcessId = $ProcessId" | ForEach-Object { $relatedPids.Add($_.ProcessId) | Out-Null }

$handles = New-Object System.Collections.Generic.List[IntPtr]
$enumProc = [Win32.Win32Utils_Debug+EnumProc]{
    param($hwnd, $lparam)
    $v = 0; [Win32.Win32Utils_Debug]::GetWindowThreadProcessId($hwnd, [ref]$v) | Out-Null
    if ($script:relatedPids.Contains($v)) { $script:handles.Add($hwnd) }
    return $true
}
[Win32.Win32Utils_Debug]::EnumWindows($enumProc, [IntPtr]::Zero) | Out-Null

$results = foreach ($hwnd in $handles) {
    try {
        $el = [System.Windows.Automation.AutomationElement]::FromHandle($hwnd)
        $elements = $el.FindAll([System.Windows.Automation.TreeScope]::Subtree, [System.Windows.Automation.Condition]::TrueCondition)
        foreach ($item in $elements) {
            try {
                $pattern = $item.GetCurrentPattern([System.Windows.Automation.TextPattern]::Pattern)
                if ($pattern) {
                    $text = $pattern.DocumentRange.GetText(20).Trim()
                    if ($text) {
                        [PSCustomObject]@{
                            WindowHandle = $hwnd
                            ElementPID   = $item.Current.ProcessId
                            ClassName    = $item.Current.ClassName
                            Name         = $item.Current.Name
                            TextHead     = $text.Replace("`n", " ").Replace("`r", "")
                        }
                    }
                }
            } catch {}
        }
    } catch {}
}

$results | Sort-Object ElementPID | Format-Table -AutoSize
param(
    [int]$TargetPid
)

Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
using System.Collections.Generic;
using System.Text;

public class Win32 {
    public delegate bool EnumWindowsProc(IntPtr hWnd, IntPtr lParam);

    [DllImport("user32.dll")]
    public static extern bool EnumWindows(EnumWindowsProc enumProc, IntPtr lParam);

    [DllImport("user32.dll")]
    public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

    [DllImport("user32.dll")]
    public static extern bool IsWindowVisible(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);

    [DllImport("user32.dll")]
    public static extern int GetWindowTextLength(IntPtr hWnd);

    public static List<string> FindWindows(int pid) {
        var list = new List<string>();
        EnumWindows((hWnd, lParam) => {
            uint procId;
            GetWindowThreadProcessId(hWnd, out procId);
            if (procId == pid) {
                int length = GetWindowTextLength(hWnd);
                var sb = new StringBuilder(length + 1);
                GetWindowText(hWnd, sb, sb.Capacity);
                bool visible = IsWindowVisible(hWnd);
                list.Add(string.Format("Handle: {0}, Visible: {1}, Title: {2}", hWnd, visible, sb.ToString()));
            }
            return true;
        }, IntPtr.Zero);
        return list;
    }
}
'@

Write-Host "Searching windows for PID $TargetPid..."
$windows = [Win32]::FindWindows($TargetPid)
if ($windows.Count -eq 0) {
    Write-Host "No windows found for PID $TargetPid."
} else {
    $windows | ForEach-Object { Write-Host $_ }
}

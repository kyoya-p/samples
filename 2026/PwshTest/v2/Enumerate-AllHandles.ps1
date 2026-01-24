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
    public static extern bool EnumChildWindows(IntPtr hWndParent, EnumWindowsProc enumProc, IntPtr lParam);

    [DllImport("user32.dll")]
    public static extern uint GetWindowThreadProcessId(IntPtr hWnd, out uint lpdwProcessId);

    [DllImport("user32.dll")]
    public static extern bool IsWindowVisible(IntPtr hWnd);

    [DllImport("user32.dll", CharSet = CharSet.Auto)]
    public static extern int GetWindowText(IntPtr hWnd, StringBuilder lpString, int nMaxCount);

    [DllImport("user32.dll", CharSet = CharSet.Auto)]
    public static extern int GetClassName(IntPtr hWnd, StringBuilder lpClassName, int nMaxCount);

    public struct WindowInfo {
        public IntPtr Handle;
        public uint ProcessId;
        public string ClassName;
        public string Title;
        public bool Visible;
        public bool IsChild;
    }

    public static List<WindowInfo> GetAllWindows() {
        var list = new List<WindowInfo>();
        
        // トップレベルウィンドウの列挙
        EnumWindows((hWnd, lParam) => {
            AddWindowToList(hWnd, list, false);
            // 子ウィンドウも再帰的に列挙
            EnumChildWindows(hWnd, (hChild, lp) => {
                AddWindowToList(hChild, list, true);
                return true;
            }, IntPtr.Zero);
            return true;
        }, IntPtr.Zero);
        
        return list;
    }

    private static void AddWindowToList(IntPtr hWnd, List<WindowInfo> list, bool isChild) {
        uint pid;
        GetWindowThreadProcessId(hWnd, out pid);
        
        var cn = new StringBuilder(256);
        GetClassName(hWnd, cn, cn.Capacity);
        
        var title = new StringBuilder(256);
        GetWindowText(hWnd, title, title.Capacity);
        
        list.Add(new WindowInfo {
            Handle = hWnd,
            ProcessId = pid,
            ClassName = cn.ToString(),
            Title = title.ToString(),
            Visible = IsWindowVisible(hWnd),
            IsChild = isChild
        });
    }
}
'@

$results = [Win32]::GetAllWindows()

if ($TargetPid) {
    Write-Host "Filtering windows for PID: $TargetPid"
    $results = $results | Where-Object { $_.ProcessId -eq $TargetPid }
}

$results | Select-Object Handle, ProcessId, Visible, IsChild, ClassName, Title | 
    Sort-Object ProcessId, IsChild

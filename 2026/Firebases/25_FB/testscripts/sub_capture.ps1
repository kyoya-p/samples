param([string]$hWndStr, [string]$filename)
$hWnd = [IntPtr][int64]$hWndStr
$code = @"
using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.Windows.Forms;
using System.Runtime.InteropServices;

public class WinAPI_Small {
    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT { public int Left; public int Top; public int Right; public int Bottom; }
}

public class WindowCaptureSub {
    public static void Capture(IntPtr hWnd, string filename) {
        WinAPI_Small.RECT rect;
        if (WinAPI_Small.GetWindowRect(hWnd, out rect)) {
            int width = rect.Right - rect.Left;
            int height = rect.Bottom - rect.Top;
            if (width > 0 && height > 0) {
                using (Bitmap bmp = new Bitmap(width, height)) {
                    using (Graphics g = Graphics.FromImage(bmp)) {
                        g.CopyFromScreen(new Point(rect.Left, rect.Top), Point.Empty, new Size(width, height));
                    }
                    bmp.Save(filename, ImageFormat.Png);
                }
            }
        }
    }
}
"@
Add-Type -TypeDefinition $code -ReferencedAssemblies "System.Drawing", "System.Windows.Forms"
[WindowCaptureSub]::Capture($hWnd, $filename)

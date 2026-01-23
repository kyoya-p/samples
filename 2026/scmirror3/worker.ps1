Add-Type @"
using System;using System.Runtime.InteropServices;using System.Text;
public struct RECT{public int Left,Top,Right,Bottom;}public struct POINT{public int x,y;}
public class Win32{
[DllImport("user32")]public static extern bool GetWindowRect(IntPtr h,out RECT r);
[DllImport("user32")]public static extern bool PostMessage(IntPtr h,uint m,IntPtr w,IntPtr l);
[DllImport("user32")]public static extern bool ScreenToClient(IntPtr h,ref POINT p);
[DllImport("user32")]public static extern IntPtr WindowFromPoint(POINT p);
[DllImport("user32")]public static extern IntPtr GetForegroundWindow();
[DllImport("user32")]public static extern int GetWindowText(IntPtr h,StringBuilder t,int n);
[DllImport("user32")]public static extern int GetClassName(IntPtr h,StringBuilder t,int n);
[DllImport("user32")]public static extern void SetProcessDPIAware();
[DllImport("user32")]public static extern void SetProcessDpiAwarenessContext(IntPtr c);
[DllImport("user32")]public static extern uint GetWindowThreadProcessId(IntPtr h,out uint p);
[DllImport("dwmapi")]public static extern int DwmGetWindowAttribute(IntPtr h,int a,out RECT r,int s);}
"@
try{[Win32]::SetProcessDpiAwarenessContext(-4)}catch{[Win32]::SetProcessDPIAware()}
[void][Reflection.Assembly]::LoadWithPartialName('System.Windows.Forms')
'{"msg":"PowerShell Ready"}'
function Get-WinInfo($h){
  $t=New-Object StringBuilder 256;$c=New-Object StringBuilder 256
  [void][Win32]::GetWindowText($h,$t,256);[void][Win32]::GetClassName($h,$c,256)
  $pid=0;[void][Win32]::GetWindowThreadProcessId($h,[ref]$pid)
  $pName="";try{$pName=(Get-Process -Id $pid -ErrorAction SilentlyContinue).Name}catch{}
  $title=$t.ToString();if(!$title){$title="(No Title)"}
  @{title=$title;class=$c.ToString();process=$pName}
}
function Make-LParam($x,$y){[IntPtr](($y -shl 16) -bor ($x -band 0xFFFF))}
function Get-Hwnd($t){if(!$t -or $t -eq '$null'){return 0}$p=ps|? MainWindowTitle -like "*$t*"|select -f 1;if($p){$p.MainWindowHandle}else{0}}
function Get-ScreenRect{$s=[System.Windows.Forms.Screen]::PrimaryScreen.Bounds;$r=New-Object RECT;$r.Right=$s.Width;$r.Bottom=$s.Height;$r}
function Get-Rect($h){$r=New-Object RECT;if([Win32]::DwmGetWindowAttribute($h,9,[ref]$r,16)-ne 0){[void][Win32]::GetWindowRect($h,[ref]$r)}$r}
function Get-Target($h,$x,$y){$p=New-Object POINT;$p.x=$x;$p.y=$y;$t=[Win32]::WindowFromPoint($p);if($t -eq 0){$t=$h}[void][Win32]::ScreenToClient($t,[ref]$p);@{h=$t;l=Make-LParam $p.x $p.y;info=Get-WinInfo $t}}
function Get-TargetGlobal($x,$y){$p=New-Object POINT;$p.x=$x;$p.y=$y;$t=[Win32]::WindowFromPoint($p);if($t -ne 0){[void][Win32]::ScreenToClient($t,[ref]$p)}@{h=$t;l=Make-LParam $p.x $p.y;info=Get-WinInfo $t}}
function Post-Msg($h,$m,$w,$l){[void][Win32]::PostMessage($h,$m,$w,$l)}
function Get-Active{[Win32]::GetForegroundWindow()}
function Get-List{ConvertTo-Json @{type="windowList";list=@(ps|? MainWindowTitle|select -Exp MainWindowTitle|sort -U)} -C}
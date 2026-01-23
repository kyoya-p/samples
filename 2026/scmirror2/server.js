const express = require('express');
const { WebSocketServer } = require('ws');
const { spawn } = require('child_process');
const path = require('path');

const app = express();

// Parse command line arguments for port
const args = process.argv.slice(2);
let port = 3000;
for (let i = 0; i < args.length; i++) {
    if ((args[i] === '-p' || args[i] === '--port') && i + 1 < args.length) {
        port = parseInt(args[i + 1], 10);
        if (isNaN(port)) port = 3000;
    }
}

app.use(express.static('.'));

app.get('/close', (req, res) => {
    res.send('Server is shutting down...');
    console.log('Received HTTP /close request. Shutting down...');
    server.close();
    wss.close();
    ps.kill();
    setTimeout(() => process.exit(0), 500);
});

const server = app.listen(port, () => {
    console.log(`Server running at http://localhost:${port}`);
});

const ps = spawn('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', '-'], {
    stdio: ['pipe', 'pipe', 'pipe']
});

ps.stderr.on('data', (data) => {
    console.error(`PS Error: ${data}`);
});

const initScript = `
Add-Type -TypeDefinition @" 
using System;
using System.Runtime.InteropServices;
public struct RECT { public int Left, Top, Right, Bottom; }
public struct POINT { public int x; public int y; }
public class User32 {
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);
    [DllImport("user32.dll")] public static extern void mouse_event(int flags, int dx, int dy, int data, int extra);
    [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
    [DllImport("user32.dll")] public static extern bool SetProcessDpiAwarenessContext(IntPtr dpiContext);
    [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr hWnd, uint Msg, IntPtr wParam, IntPtr lParam);
    [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr hWnd, ref POINT lpPoint);
    [DllImport("user32.dll")] public static extern bool ScreenToClient(IntPtr hWnd, ref POINT lpPoint);
    [DllImport("user32.dll")] public static extern IntPtr WindowFromPoint(POINT Point);
}
public class Dwmapi {
    [DllImport("dwmapi.dll")] public static extern int DwmGetWindowAttribute(IntPtr hwnd, int dwAttribute, out RECT pvAttribute, int cbAttribute);
}
"@
# Try PerMonitorV2 (-4), fallback to SystemAware if fails
try { [User32]::SetProcessDpiAwarenessContext([IntPtr]::new(-4)) | Out-Null } catch { [User32]::SetProcessDPIAware() | Out-Null }
[Reflection.Assembly]::LoadWithPartialName('System.Windows.Forms') | Out-Null

Write-Output '{"msg":"PowerShell Ready"}'

function Set-MouseAndClick([float]$relX, [float]$relY, [string]$title, [string]$type) {
    $rect = New-Object RECT
    $found = $false
    if ($title) {
        $proc = Get-Process | Where-Object { $_.MainWindowTitle.Contains($title) } | Select-Object -First 1
        if ($proc -and $proc.MainWindowHandle -ne [IntPtr]::Zero) {
            $handle = $proc.MainWindowHandle
            $DWMWA_EXTENDED_FRAME_BOUNDS = 9
            $rectSize = [System.Runtime.InteropServices.Marshal]::SizeOf([type][RECT])
            
            $res = [Dwmapi]::DwmGetWindowAttribute($handle, $DWMWA_EXTENDED_FRAME_BOUNDS, [ref]$rect, $rectSize)
            if ($res -ne 0) {
                [User32]::GetWindowRect($handle, [ref]$rect) | Out-Null
            }

            $width = $rect.Right - $rect.Left
            $height = $rect.Bottom - $rect.Top
            
            # 1. Calculate Target Screen Coordinates
            $targetScreenX = [int]($rect.Left + ($width * $relX))
            $targetScreenY = [int]($rect.Top + ($height * $relY))

            # 2. Find the actual child window at that position (e.g., Chrome Renderer)
            $ptScreen = New-Object POINT
            $ptScreen.x = $targetScreenX
            $ptScreen.y = $targetScreenY
            
            $targetHandle = [User32]::WindowFromPoint($ptScreen)
            
            if ($targetHandle -eq [IntPtr]::Zero) {
                $targetHandle = $handle # Fallback to main window
            }

            # 3. Convert Screen Coordinates to Client Coordinates for the TARGET window
            $ptClient = $ptScreen # Copy structure
            [User32]::ScreenToClient($targetHandle, [ref]$ptClient) | Out-Null
            
            $clientX = $ptClient.x
            $clientY = $ptClient.y

            $lParam = [IntPtr]($clientY -shl 16 -bor ($clientX -band 0xFFFF))
            
            if ($type -eq 'click') {
                [User32]::PostMessage($targetHandle, 0x0201, [IntPtr]1, $lParam) | Out-Null
                [User32]::PostMessage($targetHandle, 0x0202, [IntPtr]0, $lParam) | Out-Null
            } elseif ($type -eq 'move') {
                [User32]::PostMessage($targetHandle, 0x0200, [IntPtr]0, $lParam) | Out-Null
            }
            
            $found = $true 
            $info = @{
                type = "windowInfo"
                left = $rect.Left
                top = $rect.Top
                width = $width
                height = $height
            }
            Write-Output (ConvertTo-Json $info -Compress)
        }
    }
}

function Send-Key([string]$title, [string]$key) {
    if ($title) {
         $proc = Get-Process | Where-Object { $_.MainWindowTitle.Contains($title) } | Select-Object -First 1
         if ($proc -and $proc.MainWindowHandle -ne [IntPtr]::Zero) {
            $handle = $proc.MainWindowHandle
            
            if ($key.Length -eq 1) {
                $charCode = [int][char]$key
                [User32]::PostMessage($handle, 0x0102, [IntPtr]$charCode, [IntPtr]1) | Out-Null
            } elseif ($key -eq "Enter") {
                [User32]::PostMessage($handle, 0x0100, [IntPtr]13, [IntPtr]0) | Out-Null
                [User32]::PostMessage($handle, 0x0102, [IntPtr]13, [IntPtr]0) | Out-Null
                [User32]::PostMessage($handle, 0x0101, [IntPtr]13, [IntPtr]0) | Out-Null
            } elseif ($key -eq "Backspace") {
                 [User32]::PostMessage($handle, 0x0100, [IntPtr]8, [IntPtr]0) | Out-Null
                 [User32]::PostMessage($handle, 0x0102, [IntPtr]8, [IntPtr]0) | Out-Null
                 [User32]::PostMessage($handle, 0x0101, [IntPtr]8, [IntPtr]0) | Out-Null
            }
         }
    }
}

function Get-WindowList {
    $windows = Get-Process | Where-Object { $_.MainWindowTitle } | Select-Object -ExpandProperty MainWindowTitle | Sort-Object -Unique
    $response = @{
        type = "windowList"
        list = $windows
    }
    Write-Output (ConvertTo-Json $response -Compress)
}
`;

ps.stdin.write(initScript + "\n");

const wss = new WebSocketServer({ server });

ps.stdout.on('data', (data) => {
    const output = data.toString();
    console.log('PS Output:', output.trim());
    const lines = output.split('\n');
    lines.forEach(line => {
        if (line.trim().startsWith('{')) {
            wss.clients.forEach(client => {
                if (client.readyState === 1) {
                    client.send(line.trim());
                }
            });
        }
    });
});

wss.on('connection', (ws) => {
    console.log('Client connected');
    ws.on('message', (message) => {
        try {
            const data = JSON.parse(message);
            if (data.type === 'click' || data.type === 'move') {
                const title = data.title ? `'${data.title.replace(/'/g, "''")}'` : '$null';
                ps.stdin.write(`Set-MouseAndClick -relX ${data.x} -relY ${data.y} -title ${title} -type '${data.type}'\n`);
            } else if (data.type === 'key') {
                const title = data.title ? `'${data.title.replace(/'/g, "''")}'` : '$null';
                const key = data.key.replace(/'/g, "''");
                console.log(`Sending key: ${key} to window: ${title}`);
                ps.stdin.write(`Send-Key -title ${title} -key '${key}'\n`);
            } else if (data.type === 'getWindowList') {
                ps.stdin.write("Get-WindowList\n");
            }
        } catch (e) {
            console.error(e);
        }
    });
});

process.on('exit', () => ps.kill());
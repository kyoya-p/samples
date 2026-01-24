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

const server = app.listen(port, () => {
    console.log(`Server running at http://localhost:${port}`);
});

const ps = spawn('powershell.exe', ['-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', '-'], {
    stdio: ['pipe', 'pipe', 'inherit']
});

const initScript = `
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
public struct RECT { public int Left, Top, Right, Bottom; }
public class User32 {
    [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);
    [DllImport("user32.dll")] public static extern void mouse_event(int flags, int dx, int dy, int data, int extra);
}
"@
[Reflection.Assembly]::LoadWithPartialName('System.Windows.Forms') | Out-Null

function Set-MouseAndClick([float]$relX, [float]$relY, [string]$title) {
    $rect = New-Object RECT
    $found = $false
    if ($title) {
        $proc = Get-Process | Where-Object { $_.MainWindowTitle -like "*$title*" } | Select-Object -First 1
        if (-not $proc) {
            $proc = Get-Process | Where-Object { $_.MainWindowTitle -like "*YouTube*" } | Select-Object -First 1
        }
        if ($proc -and $proc.MainWindowHandle -ne [IntPtr]::Zero) {
            if ([User32]::GetWindowRect($proc.MainWindowHandle, [ref]$rect)) { 
                $found = $true 
                $info = @{
                    type = "windowInfo"
                    left = $rect.Left
                    top = $rect.Top
                    width = $rect.Right - $rect.Left
                    height = $rect.Bottom - $rect.Top
                }
                Write-Output (ConvertTo-Json $info -Compress)
            }
        }
    }
    if (-not $found) {
        $screen = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
        $rect.Left = $screen.Left; $rect.Top = $screen.Top; $rect.Right = $screen.Right; $rect.Bottom = $screen.Bottom
    }
    $width = $rect.Right - $rect.Left
    $height = $rect.Bottom - $rect.Top
    $posX = $rect.Left + ($width * $relX)
    $posY = $rect.Top + ($height * $relY)
    
    [System.Windows.Forms.Cursor]::Position = New-Object System.Drawing.Point([int]$posX, [int]$posY)
    [User32]::mouse_event(0x0002, 0, 0, 0, 0) # Left Down
    Start-Sleep -Milliseconds 50
    [User32]::mouse_event(0x0004, 0, 0, 0, 0) # Left Up
}
`;

ps.stdin.write(initScript + "\n");

const wss = new WebSocketServer({ server });

wss.on('connection', (ws) => {
    ps.stdout.on('data', (data) => {
        const lines = data.toString().split('\n');
        lines.forEach(line => {
            if (line.trim().startsWith('{')) {
                ws.send(line.trim());
            }
        });
    });

    ws.on('message', (message) => {
        try {
            const data = JSON.parse(message);
            if (data.type === 'click') {
                const title = data.title ? `'${data.title.replace(/'/g, "''")}'` : '$null';
                ps.stdin.write(`Set-MouseAndClick -relX ${data.x} -relY ${data.y} -title ${title}\n`);
            }
        } catch (e) {
            console.error(e);
        }
    });
});

process.on('exit', () => ps.kill());

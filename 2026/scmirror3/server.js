const express = require('express');
const { WebSocketServer } = require('ws');
const { spawn } = require('child_process');
const path = require('path');

const app = express();

const args = process.argv.slice(2);
let port = 3003;
for (let i = 0; i < args.length; i++) {
    if ((args[i] === '-p' || args[i] === '--port') && i + 1 < args.length) {
        port = parseInt(args[i + 1], 10);
        if (isNaN(port)) port = 3003;
    }
}

app.use(express.static(__dirname));

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

// Load the separated PowerShell script
const scriptPath = path.resolve(__dirname, 'worker.ps1');
ps.stdin.write(`. '${scriptPath}'\n`);

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
            const titleRaw = data.title;
            const title = titleRaw ? `'${titleRaw.replace(/'/g, "''")}'` : '$null';

            if (data.type === 'click' || data.type === 'move') {
                let cmd;
                if (titleRaw) {
                    // Specific Window Mode
                    cmd = `
$h = Get-Hwnd ${title};
if($h){
  $r = Get-Rect $h;
  $w = $r.Right-$r.Left; $hT = $r.Bottom-$r.Top;
  $sx = [int]($r.Left + $w*${data.x});
  $sy = [int]($r.Top + $hT*${data.y});
  $t = Get-Target $h $sx $sy;
  ${data.type === 'click' ? 
    `Post-Msg $t.h 0x0201 1 $t.l; Post-Msg $t.h 0x0202 0 $t.l;` : 
    `Post-Msg $t.h 0x0200 0 $t.l;`
  }
  Write-Output (ConvertTo-Json @{type="windowInfo";left=$r.Left;top=$r.Top;width=$w;height=$hT} -Compress)
}
`;
                } else {
                    // Full Screen Mode
                    cmd = `
$r = Get-ScreenRect;
$w = $r.Right; $hT = $r.Bottom;
$sx = [int]($w*${data.x});
$sy = [int]($hT*${data.y});
$t = Get-TargetGlobal $sx $sy;
if($t.h){
  ${data.type === 'click' ? 
    `Post-Msg $t.h 0x0201 1 $t.l; Post-Msg $t.h 0x0202 0 $t.l;` : 
    `Post-Msg $t.h 0x0200 0 $t.l;`
  }
}
Write-Output (ConvertTo-Json @{type="windowInfo";left=0;top=0;width=$w;height=$hT} -Compress)
`;
                }
                ps.stdin.write(cmd.replace(/\n/g, ' ') + "\n");
            } else if (data.type === 'key') {
                const key = data.key.replace(/'/g, "''");
                console.log(`Sending key: ${key} to window: ${title}`);
                
                let keyCmd = "";
                if (key.length === 1) {
                    const charCode = key.charCodeAt(0);
                    keyCmd = `Post-Msg $h 0x0102 ${charCode} 1`;
                } else if (key === "Enter") {
                    keyCmd = "foreach($m in 0x0100,0x0102,0x0101){Post-Msg $h $m 13 0}";
                } else if (key === "Backspace") {
                    keyCmd = "foreach($m in 0x0100,0x0102,0x0101){Post-Msg $h $m 8 0}";
                }

                if (keyCmd) {
                    const cmd = `$h = ${titleRaw ? `Get-Hwnd ${title}` : 'Get-Active'}; if($h -and $h -ne 0){ ${keyCmd} }`;
                    ps.stdin.write(cmd.replace(/\n/g, ' ') + "\n");
                }
            } else if (data.type === 'getWindowList') {
                ps.stdin.write("Get-List\n");
            }
        } catch (e) {
            console.error(e);
        }
    });
});

process.on('exit', () => ps.kill());
const http = require('http');
const fs = require('fs');
const path = require('path');
const { WebSocketServer } = require('ws');

const port = process.env.PORT || 8080;

const server = http.createServer((req, res) => {
    let filePath = '';
    let contentType = '';

    if (req.url === '/') {
        filePath = path.join(__dirname, 'index.html');
        contentType = 'text/html';
    } else if (req.url === '/client.js') {
        filePath = path.join(__dirname, 'client.js');
        contentType = 'text/javascript';
    } else if (req.url === '/config') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
            iceServers: [
                { urls: 'stun:stun.l.google.com:19302' },
                {
                    urls: process.env.TURN_SERVER_URL || '',
                    username: process.env.TURN_USERNAME || '',
                    credential: process.env.TURN_PASSWORD || ''
                }
            ]
        }));
        return;
    } else {
        res.writeHead(404);
        res.end('Not Found');
        return;
    }

    fs.readFile(filePath, (err, data) => {
        if (err) {
            res.writeHead(500);
            res.end('Error');
            return;
        }
        res.writeHead(200, { 'Content-Type': contentType });
        res.end(data);
    });
});

const wss = new WebSocketServer({ server });

wss.on('connection', (ws) => {
    console.log(`[${new Date().toLocaleTimeString()}] New signaling connection.`);
    ws.on('message', (message) => {
        wss.clients.forEach((client) => {
            if (client !== ws && client.readyState === 1) {
                client.send(message.toString());
            }
        });
    });
});

server.listen(port, () => {
    console.log(`Server started on port ${port}`);
});

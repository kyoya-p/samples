const http = require('http');
const fs = require('fs');
const path = require('path');
const { WebSocketServer } = require('ws');

const server = http.createServer((req, res) => {
    let filePath = path.join(__dirname, req.url === '/' ? 'index.html' : req.url);
    fs.readFile(filePath, (err, data) => {
        if (err) {
            res.writeHead(404);
            res.end('Not Found');
            return;
        }
        res.writeHead(200);
        res.end(data);
    });
});

const wss = new WebSocketServer({ server });

wss.on('connection', (ws) => {
    console.log(`[${new Date().toLocaleTimeString()}] New signaling connection established.`);
    
    ws.on('message', (message) => {
        const data = JSON.parse(message.toString());
        const type = data.sdp ? data.sdp.type : (data.candidate ? 'ice-candidate' : 'unknown');
        console.log(`[${new Date().toLocaleTimeString()}] Forwarding message: ${type}`);

        wss.clients.forEach((client) => {
            if (client !== ws && client.readyState === 1) {
                client.send(message.toString());
            }
        });
    });

    ws.on('close', () => {
        console.log(`[${new Date().toLocaleTimeString()}] Signaling connection closed.`);
    });
});

server.listen(4931, () => {
    console.log('Server started on http://localhost:4931');
});

const http = require('http');
const fs = require('fs');
const path = require('path');
const { WebSocketServer } = require('ws');
const Turn = require('node-turn');

// TURN サーバの起動 (Port: 3478)
const turnServer = new Turn({
    authMech: 'long-term',
    credentials: {
        'user': 'password'
    },
    debugLevel: 'INFO',
    debug: (level, message) => {
        console.log(`[TURN] ${level}: ${message}`);
    }
});
turnServer.start();
console.log('TURN Server started on port 3478');

const MIME_TYPES = {
    '.html': 'text/html',
    '.js': 'text/javascript',
    '.css': 'text/css'
};

const server = http.createServer((req, res) => {
    let urlPath = req.url === '/' ? '/index.html' : req.url;
    // セキュリティ: ディレクトリトラバーサル防止
    const safePath = path.normalize(urlPath).replace(/^(\.\.[\/\\])+/, '');
    const filePath = path.join(__dirname, safePath);

    fs.readFile(filePath, (err, data) => {
        if (err || (fs.existsSync(filePath) && fs.statSync(filePath).isDirectory())) {
            res.writeHead(404);
            res.end('Not Found');
            return;
        }
        const ext = path.extname(filePath);
        res.writeHead(200, { 'Content-Type': MIME_TYPES[ext] || 'text/plain' });
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
server.listen(8080, () => {
    console.log('Server started on http://localhost:8080');
});

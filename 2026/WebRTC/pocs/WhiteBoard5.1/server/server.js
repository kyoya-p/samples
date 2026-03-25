require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const { CommunicationIdentityClient } = require('@azure/communication-identity');
const { CommunicationRelayClient } = require('@azure/communication-network-traversal');

const app = express();
app.use(cors());
app.use(express.static('public'));

const connectionString = process.env.COMMUNICATION_SERVICES_CONNECTION_STRING;
let identityClient, relayClient;

if (connectionString) {
    try {
        console.log("Initializing ACS clients with connection string (length: " + connectionString.length + ")");
        identityClient = new CommunicationIdentityClient(connectionString);
        relayClient = new CommunicationRelayClient(connectionString);
        console.log("Azure Communication Services initialized successfully.");
    } catch (e) {
        console.error("CRITICAL: Failed to initialize ACS clients:", e.message);
    }
} else {
    console.warn("WARNING: COMMUNICATION_SERVICES_CONNECTION_STRING is missing.");
}

app.get('/ice-servers', async (req, res) => {
    const staticServers = [{ urls: 'stun:stun.l.google.com:19302' }];
    
    // 手動設定のTURNサーバーがあれば追加
    if (process.env.TURN_URL) {
        staticServers.push({
            urls: process.env.TURN_URL,
            username: process.env.TURN_USER || 'user',
            credential: process.env.TURN_PASSWORD || 'password123'
        });
        
        // 開発環境用のローカルホスト候補（もし必要なら）
        if (process.env.TURN_URL.includes('127.0.0.1') || process.env.TURN_URL.includes('localhost')) {
             // すでにあるので何もしない
        }
    }

    if (!relayClient) {
        return res.json(staticServers);
    }

    try {
        const user = await identityClient.createUser();
        // 最新のSDKに合わせてオブジェクト形式で渡す
        const config = await relayClient.getRelayConfiguration({ communicationUserIdentifier: user });
        return res.json([...config.iceServers, ...staticServers]);
    } catch (error) {
        console.warn("ACS Relay failed, using fallback static servers:", error.message);
        res.json(staticServers);
    }
});

// シグナリングサーバーとHTTPサーバーのセットアップ
function createServerInstance(port) {
    const server = http.createServer(app);
    const io = new Server(server, { 
        cors: { origin: "*" } 
    });

    io.on('connection', (socket) => {
        socket.on('join', (room) => {
            socket.join(room);
            console.log(`Socket ${socket.id} joined room: ${room}`);
            // 部屋の人数をログ
            const clients = io.sockets.adapter.rooms.get(room);
            console.log(`Room ${room} now has ${clients ? clients.size : 0} clients`);
        });

        socket.on('signal', (data) => {
            if (data.room) {
                console.log(`Relaying signal from ${socket.id} to room ${data.room}: type=${data.type}`);
                socket.to(data.room).emit('signal', data);
            }
        });

        socket.on('disconnect', () => {
            console.log(`Socket ${socket.id} disconnected from port ${port}`);
        });
    });

    server.listen(port, '0.0.0.0', () => {
        console.log(`Server instance running on port ${port}`);
    });
}

// メインポート (デフォルト3000) で起動
const PORT = process.env.PORT || 3000;
createServerInstance(PORT);

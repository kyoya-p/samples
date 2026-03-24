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
    identityClient = new CommunicationIdentityClient(connectionString);
    relayClient = new CommunicationRelayClient(connectionString);
}
app.get('/ice-servers', async (req, res) => {
    const staticServers = [{ urls: 'stun:stun.l.google.com:19302' }];
    if (process.env.TURN_URL) {
        staticServers.push({
            urls: process.env.TURN_URL,
            username: process.env.TURN_USER || 'user',
            credential: process.env.TURN_PASSWORD || 'password123'
        });
        staticServers.push({
            urls: process.env.TURN_URL.replace(/172\.\d+\.\d+\.\d+/, 'localhost'),
            username: process.env.TURN_USER || 'user',
            credential: process.env.TURN_PASSWORD || 'password123'
        });
    }
    if (!relayClient) return res.json(staticServers);
    try {
        const user = await identityClient.createUser();
        const config = await relayClient.getRelayConfiguration({ communicationUserIdentifier: user });
        return res.json([...config.iceServers, ...staticServers]);
    } catch (error) {
        console.warn("ACS Relay failed, using fallback static servers:", error.message);
        res.json(staticServers);
    }
});

function setupServer(port) {
    const server = http.createServer(app);
    const io = new Server(server, { cors: { origin: "*" } });
    io.on('connection', (socket) => {
        socket.on('join', (room) => socket.join(room));
        socket.on('signal', (data) => {
            if (data.room) socket.to(data.room).emit('signal', data);
        });
    });
    server.listen(port, '0.0.0.0', () => console.log(`Server running on port ${port}`));
}

// 指示通り、独立したポート 8081, 8082, 3000 で起動
[3000, 8081, 8082].forEach(p => setupServer(p));

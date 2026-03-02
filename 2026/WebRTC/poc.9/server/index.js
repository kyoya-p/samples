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
    const staticStun = [{ urls: 'stun:stun.l.google.com:19302' }];
    if (!relayClient) return res.json(staticStun);

    let lastError = null;
    // Azure 側の構成伝播待ちを考慮し、最大3回リトライ
    for (let i = 0; i < 3; i++) {
        try {
            const user = await identityClient.createUser();
            // 一部の SDK バージョンやリージョンでの挙動を考慮し、user を直接渡す
            const config = await relayClient.getRelayConfiguration(user);
            console.log("ACS Relay Configuration fetched successfully on attempt", i + 1);
            return res.json([...config.iceServers, ...staticStun]);
        } catch (error) {
            lastError = error;
            console.warn(`ACS Relay Attempt ${i + 1} failed:`, error.message);
            if (error.message.includes("404")) {
                // 404 の場合は構成待ちの可能性があるため、少し待機してリトライ
                await new Promise(resolve => setTimeout(resolve, 2000));
                continue;
            }
            break; 
        }
    }

    console.error("ACS Relay Final Error:", lastError ? lastError.message : "Unknown error");
    res.json(staticStun);
});

const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

io.on('connection', (socket) => {
    socket.on('join', (room) => socket.join(room));
    socket.on('signal', (data) => socket.to(data.room).emit('signal', data));
});

server.listen(3000, () => console.log('Server running on port 3000'));

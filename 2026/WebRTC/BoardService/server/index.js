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

    try {
        const user = await identityClient.createUser();
        const config = await relayClient.getRelayConfiguration(user);
        res.json([...config.iceServers, ...staticStun]);
    } catch (error) {
        console.error("ACS Relay Error:", error.message);
        res.json(staticStun);
    }
});

const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

io.on('connection', (socket) => {
    socket.on('join', (room) => socket.join(room));
    socket.on('signal', (data) => socket.to(data.room).emit('signal', data));
});

server.listen(3000, () => console.log('Server running on port 3000'));

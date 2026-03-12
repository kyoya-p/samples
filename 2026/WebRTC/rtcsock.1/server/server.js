require('dotenv').config();
const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');

const path = require('path');
const app = express();
app.use(cors());
app.use(express.static(path.join(__dirname, 'public')));
app.use('/node_modules', express.static(path.join(__dirname, '../node_modules')));

app.get('/ice-servers', async (req, res) => {
    const staticServers = [{ urls: 'stun:stun.l.google.com:19302' }];
    if (process.env.TURN_URL) {
        staticServers.push({
            urls: process.env.TURN_URL,
            username: process.env.TURN_USER || 'user',
            credential: process.env.TURN_PASSWORD || 'password123'
        });
    }
    res.json(staticServers);
});

const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });

io.on('connection', (socket) => {
    socket.on('join', (room) => {
        socket.join(room);
        console.log(`Socket ${socket.id} joined room: ${room}`);
    });

    socket.on('signal', (data) => {
        if (data.room) {
            socket.to(data.room).emit('signal', data);
        }
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, '0.0.0.0', () => {
    console.log(`Signaling server running on port ${PORT}`);
});

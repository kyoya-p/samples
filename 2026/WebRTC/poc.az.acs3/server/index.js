const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');

const app = express();
app.use(cors());
app.use(express.static('public'));

app.get('/ice-servers', async (req, res) => {
    console.log("Request received for /ice-servers (ULTRA SIMPLE)");
    // Pure static response to avoid ANY SDK errors
    res.json([
        { urls: 'stun:stun.l.google.com:19302' }
    ]);
});

const server = http.createServer(app);
const io = new Server(server, { 
    cors: { origin: "*" },
    allowEIO3: true // Support older clients if any
});

io.on('connection', (socket) => {
    console.log("Socket connected:", socket.id);
    socket.on('join', (room) => {
        console.log(`Socket ${socket.id} joining room: ${room}`);
        socket.join(room);
    });
    socket.on('signal', (data) => {
        console.log(`Signal from ${socket.id} to room ${data.room}: type=${data.type}`);
        socket.to(data.room).emit('signal', data);
    });
    socket.on('disconnect', () => console.log("Socket disconnected:", socket.id));
});

server.listen(3000, () => console.log('Server running on port 3000'));

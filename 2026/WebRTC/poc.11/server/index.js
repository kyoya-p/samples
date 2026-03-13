const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const path = require('path');

const app = express();
const server = http.createServer(app);
const io = new Server(server, {
    cors: { origin: "*" }
});

app.use(express.static(path.join(__dirname, 'public')));

io.on('connection', (socket) => {
    console.log('User connected:', socket.id);

    socket.on('join', (roomName) => {
        socket.join(roomName);
        console.log(`User ${socket.id} joined room: ${roomName}`);
        socket.to(roomName).emit('user-joined', socket.id);
    });

    socket.on('offer', ({ offer, roomName }) => {
        socket.to(roomName).emit('offer', { offer, from: socket.id });
    });

    socket.on('answer', ({ answer, roomName }) => {
        socket.to(roomName).emit('answer', { answer, from: socket.id });
    });

    socket.on('ice-candidate', ({ candidate, roomName }) => {
        socket.to(roomName).emit('ice-candidate', { candidate, from: socket.id });
    });

    socket.on('disconnect', () => {
        console.log('User disconnected:', socket.id);
    });
});

const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
    console.log(`Signaling server running on http://localhost:${PORT}`);
});

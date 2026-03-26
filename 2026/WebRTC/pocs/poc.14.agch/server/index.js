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

    socket.on('offer', ({ offer, to }) => {
        socket.to(to).emit('offer', { offer, from: socket.id });
    });

    socket.on('answer', ({ answer, to }) => {
        socket.to(to).emit('answer', { answer, from: socket.id });
    });

    socket.on('ice-candidate', ({ candidate, to }) => {
        socket.to(to).emit('ice-candidate', { candidate, from: socket.id });
    });

    socket.on('disconnect', () => {
        console.log('User disconnected:', socket.id);
    });
});

const PORT = process.env.PORT || 49880;
server.listen(PORT, () => {
    console.log(`Signaling server running on port ${PORT}`);
});

// --- TCP Echo Server ---
const net = require('net');
const tcpEchoServer = net.createServer((socket) => {
    console.log('TCP Echo connection from:', socket.remoteAddress);
    socket.pipe(socket);
});
tcpEchoServer.listen(parseInt(PORT) + 1, () => {
    console.log(`TCP Echo server running on port ${parseInt(PORT) + 1}`);
});

// --- UDP Echo Server ---
const dgram = require('dgram');
const udpEchoServer = dgram.createSocket('udp4');
udpEchoServer.on('message', (msg, rinfo) => {
    console.log(`UDP Echo message from ${rinfo.address}:${rinfo.port}: ${msg.toString().trim()}`);
    udpEchoServer.send(msg, rinfo.port, rinfo.address);
});
udpEchoServer.on('listening', () => {
    const address = udpEchoServer.address();
    console.log(`UDP Echo server listening on ${address.address}:${address.port}`);
});
udpEchoServer.bind(PORT);

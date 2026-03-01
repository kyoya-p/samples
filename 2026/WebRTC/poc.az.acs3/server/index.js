const express = require('express');
const http = require('http');
const { Server } = require('socket.io');
const cors = require('cors');
const app = express();
app.use(cors());
app.use(express.static('public'));
const server = http.createServer(app);
const io = new Server(server, { cors: { origin: "*" } });
io.on('connection', (socket) => {
    socket.on('join', (room) => socket.join(room));
    socket.on('signal', (data) => socket.to(data.room).emit('signal', data));
});
server.listen(3000, () => console.log('Server running on port 3000'));

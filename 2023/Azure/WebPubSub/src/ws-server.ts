import { WebSocketServer, WebSocket } from 'ws';

const port = 3000;
const server = new WebSocketServer({ port });

server.on('connection', (socket: WebSocket) => {
    console.log('Client connected');
    socket.on('message', (data: string) => { console.log(`Received: ${data}`); });
    socket.on('close', () => { console.log('Client disconnected'); });
});

console.log('start service.');

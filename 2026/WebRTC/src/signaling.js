const { WebSocketServer } = require('ws');

const wss = new WebSocketServer({ port: 4932 });

console.log('Signaling server started on ws://localhost:4932');

wss.on('connection', (ws) => {
  ws.on('message', (message) => {
    // シンプルなブロードキャスト（自分以外に送信）
    wss.clients.forEach((client) => {
      if (client !== ws && client.readyState === 1) {
        client.send(message.toString());
      }
    });
  });
});

import express from 'express';
import WebSocket from 'ws';

// シグナリングサーバを作成
const app = express();
const wss = new WebSocket();

// 接続イベントハンドラを登録
wss.on('connection', (ws) => {
  // 接続したクライアントを保存
  clients.push(ws);

  // クライアントからメッセージが届いたら、他のクライアントに送信
  ws.on('message', (message) => {
    for (const client of clients) {
      if (client !== ws) {
        client.send(message);
      }
    }
  });

  // クライアントが切断したら、リストから削除
  ws.on('close', () => {
    clients.splice(clients.indexOf(ws), 1);
  });
});

// シグナリングサーバを起動
app.listen(3000, () => {
  console.log('Signaling server started on port 3000');
});

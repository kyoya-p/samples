import { WebSocket, WebSocketServer } from "ws";

const wss = new WebSocketServer({ port: 7777 });

wss.on("connection", (ws: WebSocket) => {
  console.log("connected!");

  ws.on("message", (data, isBinary) => {
    for (const client of wss.clients) {
      if (client.readyState === WebSocket.OPEN) {
        client.send(data, { binary: isBinary });
      }
    }
  });

  ws.on("close", () => console.log("closed!"));
});

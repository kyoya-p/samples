import express from "express";
import http from "http";
import { Server } from "socket.io";
import { Page } from 'puppeteer';
import { capture2 } from ".";

export async function runServer2(page: Page, port: number = 3000) {

  const app = express();
  const httpServer = http.createServer(app);
  const io = new Server(httpServer);

  app.use(express.static("."));
  io.on("connection", (socket) => {
    console.log("Connected.");
    setInterval(async () => {
      const hash = await capture2(page)
      if (hash) {
        socket.emit("server_message", `image.png?h=${hash}`);
      }
    }, 5000);

  });
  app.get('/op/click', async (req: any, res: { send: (arg0: string) => void; }) => {
    const x = parseInt(req.query.x)
    const y = parseInt(req.query.y)
    console.log(`Clicked(${x},${y})`)
    page.mouse.click(x, y)
    capture2(page)
    res.send(`{}`)
  });

  httpServer.listen(port, () => {
    console.log("Listen start.");
  });

}
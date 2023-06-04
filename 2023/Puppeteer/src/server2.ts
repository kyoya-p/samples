import express from "express";
import http from "http";
import { Server } from "socket.io";
import { Page } from 'puppeteer';
import { capture, capture2 } from ".";

export async function runServer2(page: Page, port: number = 3000) {

  const app = express();
  const httpServer = http.createServer(app);
  const io = new Server(httpServer);

  app.use(express.static("."));
  app.get('/op/click', async (req: any, res: { send: (arg0: string) => void; }) => {
    const x = parseInt(req.query.x)
    const y = parseInt(req.query.y)
    console.log(`Clicked(${x},${y})`)
    await page.mouse.click(x, y)
    await capture(page)
    console.log(`image updated.`)
    res.send(`{}`)
  });

  io.on("connection", (socket) => {
    console.log("Connected.");
    setInterval(async () => {
      if (await capture2(page)) {
        socket.emit("server_message", "image.png");
      }
    }, 1000);

  });

  httpServer.listen(port, () => {
    console.log("Listen start.");
  });

}
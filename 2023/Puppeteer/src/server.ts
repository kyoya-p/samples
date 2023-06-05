import express from "express";
import http from "http";
import { Server } from "socket.io";
import { Page } from 'puppeteer';
import { capture2 } from ".";

export async function runServer2(page: Page, port: number = 3000) {

  const app = express();
  const httpServer = http.createServer(app);
  const server = new Server(httpServer);

  let timer: NodeJS.Timer | undefined
  app.use(express.static("."));
  server.on("connection", (socket) => {
    console.log("connected.");
    socket.timeout(120 * 1000).emit("my", () => {
      console.log("timed-out.")
      clearInterval(timer)
    })
    timer = setInterval(async () => {
      const hash = await capture2(page)
      if (hash) {
        socket.emit("server_message", `image.png?h=${hash}`);
      }
    }, 2000);
  });
  server.on("disconnect", () => {
    console.log("disconnect.");
    clearInterval(timer);
  });

  app.get('/op/click', async (req: any, res: { send: (arg0: string) => void; }) => {
    const x = parseInt(req.query.x)
    const y = parseInt(req.query.y)
    console.log(`Clicked(${x},${y})`)
    //    page.mouse.click(x, y)
    page.mouse.move(x, y)
    page.mouse.down({ button: "left" })
    page.mouse.up({ button: "left" })
    const hash = await capture2(page)
    if (hash) {
      console.log(`{"img":"image.png?h=${hash}"}`)
      res.send(`{"img":"image.png?h=${hash}"}`)
    } else {
      res.send(`{}`)
    }
  });

  httpServer.listen(port, () => {
    console.log(`Listen start http://localhost:${port}/`);
  });

}
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
    }, 5000);
  });
  server.on("disconnect", () => {
    console.log("disconnect.");
    clearInterval(timer);
  });

  app.get('/op/click', async (req: any, res: { send: (arg0: string) => void; }) => {
    const x = parseInt(req.query.x)
    const y = parseInt(req.query.y)
    console.log(`Clicked(${x},${y})`)
    await page.mouse.click(x, y)
    // page.mouse.down({ button: "left" })
    // page.mouse.up({ button: "left" })

    let response = `{}`
    // const hash = await capture2(page)
    // if (hash) {
    //   console.log(`{"img":"image.png?h=${hash}"}`)
    //   response = `{"img":"image.png?h=${hash}"}`
    // } 
    res.send(response)
  });
  app.get('/op/mousedown', async (req: any, res: { send: (arg0: string) => void; }) => {
    const x = parseInt(req.query.x)
    const y = parseInt(req.query.y)
    console.log(`mousedown(${x},${y})`)
    let response = `{}`
    // await page.mouse.move(x, y)
    await page.mouse.down({ button: "left", })
    res.send(response)
  });
  app.get('/op/mouseup', async (req: any, res: { send: (arg0: string) => void; }) => {
    const x = parseInt(req.query.x)
    const y = parseInt(req.query.y)
    console.log(`mouseup(${x},${y})`)
    let response = `{}`
    // await page.mouse.move(x, y)
    await page.mouse.up({ button: "left", })
    res.send(response)
  });
  app.get('/op/mousemove', async (req: any, res: { send: (arg0: string) => void; }) => {
    const x = parseInt(req.query.x)
    const y = parseInt(req.query.y)
    console.log(`mousemove(${x},${y})`)
    let response = `{}`
    await page.mouse.move(x, y)
    res.send(response)
  });

  httpServer.listen(port, () => {
    console.log(`Listen start http://localhost:${port}/`);
  });

}
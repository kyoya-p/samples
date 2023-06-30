import fs from "fs"
import * as crypto from "crypto";
import express from "express"
import NodeMediaServer from 'node-media-server'
import { Page, chromium, devices } from 'playwright'

main()

async function main() {
  const tgUrl = process.argv[2] ?? "https://google.com"
  const port = parseInt(process.argv[3] ?? "3000")

  const browser = await chromium.launch({
    //headless: false,
    //devtools: true,
    args: ["--remote-debugging-port=9222","--remote-debugging-address=0.0.0.0"]
  })
  // const ctx = await browser.newContext({ recordVideo: { dir: "./result" , } })
  const ctx = await browser.newContext()
  const page = await ctx.newPage()
  await page.goto(tgUrl)

  webServer(page, port)
  rtmpServer()
  setInterval(async () => { capture(page) }, 2000)
}

let hash = ""

async function capture(page: Page) {
  const newImg = Buffer.from(await page.screenshot())
  hash = crypto.createHash("sha256").update(newImg).digest().toString("hex")
  fs.writeFileSync(`result/screenshot_tmp.png`, newImg)
  fs.renameSync(`result/screenshot_tmp.png`, `result/screenshot.png`)
}

async function webServer(page: Page, port: number) {
  const app = express()
  app.use(express.static("."));
  app.get("", (req, res) => { res.send(`<script src="client.js"></script><img width="100%" height="100%" id="img" onclick="handleClick(event)"></img>`) })
  app.get("/click", async (req: any, res) => {
    page.mouse.click(parseInt(req.query.x), parseInt(req.query.y), { button: "left" })
    console.log(`/click(${req.query.x},${req.query.y})`)
    await capture(page)
    res.send(`{hash:${hash}}`)
  })
  app.get("/hash", (req, res) => { res.send(`{"hash":"${hash}"}`) })
  app.listen(port, () => { console.log(`Server is running on port ${port}`) })
}

async function rtmpServer() {
  const config = {
    rtmp: {
      port: 1935,
      chunk_size: 60000,
      gop_cache: true,
      ping: 30,
      ping_timeout: 60
    },
    http: {
      port: 8000,
      allow_origin: '*',
      mediaroot: './media',
    }
  }

  var nms = new NodeMediaServer(config)
  nms.run()
}
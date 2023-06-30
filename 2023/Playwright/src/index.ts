import fs from "fs"
import * as crypto from "crypto";
import express from "express"
import { Page, chromium, devices } from 'playwright'

main()

async function main() {
  const tgUrl = process.argv[2] ?? "https://google.com"
  const port = parseInt(process.argv[3] ?? "3000")

  const browser = await chromium.launch()
  const page = await browser.newPage()
  await page.goto(tgUrl)

  webServer(page, port)
  setInterval(async () => { capture(page) }, 2000)
}

let hash = ""

async function capture(page: Page) {
  const newImg = Buffer.from(await page.screenshot())
  hash = crypto.createHash("sha256").update(newImg).digest().toString("hex")
  fs.writeFileSync(`result/screenshot.png`, newImg)
}

async function webServer(page: Page, port: number) {
  const app = express()
  app.use(express.static("."));
  app.get("", (req, res) => { res.send(`<script src="client.js"></script><img width="100%" height="100%" id="img" onclick="handleClick(event)"></img>`) })
  app.get("/click", (req: any, res) => {
    page.mouse.click(parseInt(req.query.x), parseInt(req.query.y), { button: "left" })
    res.send(`{}`)
    console.log(`/click(${req.query.x},${req.query.y})`)
  })
  app.get("/hash", (req, res) => { res.send(`{"hash":"${hash}"}`) })
  app.listen(port, () => { console.log(`Server is running on port ${port}`) })
}
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
  setInterval(async () => { page.screenshot({ path: 'result/screenshot.png' }) }, 2000)

  // await browser.close()
}

let hash = "" //TODO

async function webServer(page: Page, port: number) {
  const app = express()
  app.use(express.static("."));
  app.get("/click", (req: any, res) => {
    page.mouse.click(parseInt(req.query.x), parseInt(req.query.y), { button: "left" })
    res.send(`{}`)
  })
  app.get("", (req, res) => {
    res.send(`
    <script src="client.js"></script>
    <img src="result/screenshot.png?${hash}" onclick="handleClick(event)"></img>`)
  })
  app.listen(port, () => { console.log(`Server is running on port ${port}`) })
}
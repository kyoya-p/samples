import { Page, chromium, devices } from 'playwright'
import express from "express"

main()

async function main() {

  const browser = await chromium.launch()
  const page = await browser.newPage()
  await page.goto('https://google.com/')

  webServer(page)
  setInterval(async () => { page.screenshot({ path: 'result/screenshot.png' }) }, 2000)

  // await browser.close()
}

async function capture(page: Page) {
  await page.screenshot({ path: 'result/screenshot.png' })
}

let hash = ""

async function webServer(page: Page) {
  const app = express()
  app.use(express.static("."));
  app.get("/click", (req: any, res) => {
    const x = parseInt(req.query.x)
    const y = parseInt(req.query.y)
    page.mouse.click(x, y, { button: "left" })
    res.send(`{}`)
  })
  app.get("", (req, res) => {
    res.send(`
    <script src="client.js"></script>
    <img src="result/screenshot.png?${hash}"  onclick="handleClick(event)"></img>`)
  })
  app.listen(3000, () => { console.log("Server is running on port 3000") })
}
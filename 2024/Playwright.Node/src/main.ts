import Express from "express"
import Playwright from "playwright"

const app = Express()
app.get("/", runBrowser)
app.listen(8080, () => { console.log(`Running server.`); })

async function runBrowser(req: Express.Request, res: Express.Response) {
  const browser = await Playwright.chromium.launch({ headless: true })
  const page = await browser.newPage()
  await page.goto("https://digital.onl.jp/")
  const screenshot = await page.screenshot()
  res.setHeader("Content-Type", "image/png")
  res.send(screenshot)
  await browser.close()
}


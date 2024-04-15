import Express from "express"
import Playwright from "playwright"
import { test, expect } from '@playwright/test';

test('test', async ({ page }) => {
  await page.goto('https://dev7-smartoffice.sharpb2bcloud.com/');
  await page.goto('https://dev7-smartoffice.sharpb2bcloud.com/#/');
  await page.goto('https://fmw7-2.us.auth0.com/login?state=hKFo2SBBQTN6dW5PS3J4YXFIUFNQQlF5ZjgyMlJlLWpZeFFoQqFupWxvZ2lu\
o3RpZNkgV3VJMjdHNDNpaUtQUjc2MG9aTUhmLWVETVE2UW1UNTmjY2lk2SBNb3FNZk1MRlNwWGJIa1RWTndyTExuRzgzSmo4VktSMA&client=MoqMfMLFS\
pXbHkTVNwrLLnG83Jj8VKR0&protocol=oauth2&response_type=token%20id_token&redirect_uri=https%3A%2F%2Fdev7-smartoffice.shar\
pb2bcloud.com%2F%23%2Fcallback&scope=openid%20profile&audience=https%3A%2F%2Fmarvel-portal.invalid&ms_scope=openid%2Cpr\
ofile%2Cemail%2Coffline_access%2CUser.Read%2CUser.ReadBasic.All&gw_scope=https%3A%2F%2Fwww.googleapis.com%2Fauth%2Fadmi\
n.directory.domain.readonly%2Chttps%3A%2F%2Fwww.googleapis.com%2Fauth%2Fadmin.directory.group.readonly%2Chttps%3A%2F%2F\
www.googleapis.com%2Fauth%2Fadmin.directory.resource.calendar.readonly%2Chttps%3A%2F%2Fwww.googleapis.com%2Fauth%2Fadmi\
n.directory.user.readonly%2Chttps%3A%2F%2Fwww.googleapis.com%2Fauth%2Fuserinfo.profile&nonce=g3Q1EbzC8erB~1u7vdXgJYiQtk\
YQCFqc&auth0Client=eyJuYW1lIjoiYXV0aDAuanMiLCJ2ZXJzaW9uIjoiOS4xOS4wIn0%3D');
  await page.getByPlaceholder('name@example.com').click();
  await page.getByPlaceholder('name@example.com').fill('kyoya.coc1');
  await page.getByPlaceholder('name@example.com').press('Tab');
  await page.locator('a').press('Enter');
  await page.getByLabel('Password:').fill('$harp123');
  await page.getByLabel('Password:').press('Enter');
  await page.getByRole('link', { name: ' kyoya coc1' }).click();
  await page.getByRole('link', { name: ' Log Out' }).click();
});





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


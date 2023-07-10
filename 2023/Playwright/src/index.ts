import { Page, chromium, devices } from 'playwright';
import assert from 'node:assert';
import { setMaxIdleHTTPParsers } from 'node:http';

main()

async function main() {
  const browser = await chromium.launch();
  const page = await browser.newPage();
  await page.goto('https://google.com/');

  setInterval(async () => {capture(page)}, 3000);

  await browser.close();
}

async function capture(page:Page) {
    await page.screenshot({path: 'result/screenshot.png'});
}
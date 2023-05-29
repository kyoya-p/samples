/*
headless Webブラウザ上で画面が更新された場合に画像を連続的に保存するTSコードサンプル

*/

import puppeteer, { Page } from "puppeteer";
import fs from "fs";

main();

async function main() {
    const browser = await puppeteer.launch({
        headless: 'new',
        // slowMo: 500,
    });
    const page = await browser.newPage();

     page.goto("https://google.com");
    capture(page)
    clickerTest(page)
    // await browser.close();
}

async function capture(page: Page) {
    let i = 0
    while (true) {
        await page.waitForNavigation()
        const image = await page.screenshot();
        fs.writeFileSync(`#image-${i++}.png`, Uint8Array.from(image))
        console.log("image update.")
    }
}

async function clickerTest(page: Page) {
    while (true) {
        await page.waitForNavigation()
        await sleep(10000)
        const mouse = await page.mouse;
        await mouse.click(0, 0)
        console.log("clicked.")

    }
}

async function sleep(delay: number) {
    await new Promise(resolve => setTimeout(resolve, delay))
}

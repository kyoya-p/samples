/*
headless Chromeで画面が更新時キャプチャするTSコードサンプル

*/

import express from "express";
import puppeteer, { Page } from "puppeteer";
import fs from "fs";

main();

async function main() {
    const browser = await puppeteer.launch({
        headless: 'new',
        // slowMo: 500,
    });
    const page = await browser.newPage();
    console.log("start.")

    page.goto("https://google.com");
    capture(page)
    clickerTest(page)
    // await browser.close();
}

async function capture(page: Page) {
    let i = 0
    while (true) {
        await Promise.race([page.waitForNavigation(), sleep(10000)])
        const image = await page.screenshot();
        fs.writeFileSync(`#image-${i++}.png`, Uint8Array.from(image))
        console.log("image update.")
    }
}

async function clickerTest(page: Page) {
    while (true) {
        await page.waitForNavigation()
        await sleep(10000)
        const mouse = await page.mouse
        await mouse.click(0, 0)
        console.log("clicked.")
    }
}

async function sleep(delay: number) {
    await new Promise(resolve => setTimeout(resolve, delay))
}
async function webServer() {
    const server = express()
    server.listen(3000)
}

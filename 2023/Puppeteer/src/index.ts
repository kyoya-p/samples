/*
headless Webブラウザを使用し、画像を連続的に保存するTSコードサンプル
*/

import fs from "fs";
import express from "express";
import puppeteer, { Page } from "puppeteer";
import { runServer2 } from "./server2";

main()

async function main() {
    const browser = await puppeteer.launch({
        headless: 'new',
        // slowMo: 500,
        ignoreHTTPSErrors: true,
        // args: ['--ignore-certificate-errors','--proxy-server=http://proxy-nara.jp.sharp:3080'],
        args: [process.env.PROXY].filter(e => e) as string[],
    });

    const page = await browser.newPage();
    console.log("start.")
    console.log(`${process.argv}`)
    runServer2(page, Number(process.argv[3] ?? "3000"))

    await page.authenticate({ username: process.env.USER ?? "", password: process.env.PASSWORD ?? "" });
    page.goto(process.argv[2] ?? "https://www.coolmathgames.com/ja/0-reversi");
    //setInterval(() => capture(page), 1000);

    // await browser.close();
}

export async function capture(page: Page) {
    fs.writeFileSync(`image.png`, Uint8Array.from(await page.screenshot()))
    console.log(`update image.`)
}
let img: Buffer
export async function capture2(page: Page) {
    const newImg = Buffer.from(await page.screenshot())
    if (newImg !== img) {
        fs.writeFileSync(`image.png`, Buffer.from(await page.screenshot()))
        img = newImg
        console.log(`update image.`)
        return true
    }
    return false
}

async function sleep(delay: number) {
    await new Promise(resolve => setTimeout(resolve, delay))
}

async function runServer(page: Page, port: number = 3000) {
    const app = express();
    app.use(express.static("."));
    app.get('/op/click', async (req: any, res: { send: (arg0: string) => void; }) => {
        const x = parseInt(req.query.x)
        const y = parseInt(req.query.y)
        console.log(`Clicked(${x},${y})`)
        await page.mouse.click(x, y)
        await capture(page)
        console.log(`image updated.`)
        res.send(`{}`)
    });
    app.listen(port, () => console.log(`Server listening at http://localhost:${port} `));
}

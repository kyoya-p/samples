/*
headless Webブラウザを使用し、画像を連続的に保存するTSコードサンプル
*/

import puppeteer, { Page } from "puppeteer";
import fs from "fs";
import express from 'express';

main()

async function main() {
    const browser = await puppeteer.launch({
        headless: 'new',
        // slowMo: 500,
    });
    const page = await browser.newPage();
    console.log("start.")
    runServer(page)

    page.goto("https://www.coolmathgames.com/ja/0-reversi");
    // capture(page)
    setInterval(() => capture(page), 1000);

    // await browser.close();
}

async function capture(page: Page) {
    fs.writeFileSync(`image.png`, Uint8Array.from(await page.screenshot()))
}

async function sleep(delay: number) {
    await new Promise(resolve => setTimeout(resolve, delay))
}

async function runServer(page: Page) {

    const app = express();
    const port = 3000;

    app.use(express.static("."));

    app.get('/op/click', async (req: any, res: { send: (arg0: string) => void; }) => {
        const x = parseInt(req.query.x)
        const y = parseInt(req.query.y)
        console.log(`Clicked(${x},${y})`)
        await page.mouse.click(x, y)
        await capture(page)
        // await fs.writeFileSync(`image.png`, Uint8Array.from(await page.screenshot()))
        console.log(`image updated.`)
        res.send(`{}`)
    });

    app.listen(port, () => {
        console.log(`Server listening at http://localhost:${port} `);
    });

}

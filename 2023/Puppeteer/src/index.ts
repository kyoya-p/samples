/*
headless Webブラウザ上で画面が更新された場合に画像を連続的に保存するTSコードサンプル
*/

import puppeteer, { Page } from "puppeteer";
import fs from "fs";
import express from 'express';
import path from 'path';

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
    // page.goto("https://google.com");
    await capture(page)
    await browser.close();
}

async function capture(page: Page) {
    while (true) {
        const image = await page.screenshot();
        fs.writeFileSync(`image.png`, Uint8Array.from(image))
        console.log("image update.")
        await sleep(1000)
    }
}

async function sleep(delay: number) {
    await new Promise(resolve => setTimeout(resolve, delay))
}




async function runServer(page: Page) {

    const app = express();
    const port = 3000;

    // Serve static files
    app.use(express.static("."));

    // Define API routes
    app.get('/op/click', async (req: any, res: { send: (arg0: string) => void; }) => {
        const x = parseInt(req.query.x)
        const y = parseInt(req.query.y)
        console.log(`Clicked(${x},${y})`)
        await page.mouse.click(x, y)
        await fs.writeFileSync(`image.png`, Uint8Array.from(await page.screenshot()))
        console.log(`image updated.`)
        res.send(`{}`)
    });

    // Start the server
    app.listen(port, () => {
        console.log(`Server listening at http://localhost:${port} `);
    });

}

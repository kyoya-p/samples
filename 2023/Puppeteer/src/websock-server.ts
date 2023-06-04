/*
headless Webブラウザを使用し、画像を連続的に保存するTSコードサンプル
*/

import fs from "fs";
import express from "express";
import puppeteer, { Page } from "puppeteer";
import WebSocket from "ws";

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
    runServer(page, Number(process.argv[3] ?? "3000"))

    await page.authenticate({ username: process.env.USER ?? "", password: process.env.PASSWORD ?? "" });
    page.goto(process.argv[2] ?? "https://www.coolmathgames.com/ja/0-reversi");
    setInterval(() => capture(page), 1000);

    // await browser.close();
}

async function capture(page: Page) {
    fs.writeFileSync(`image.png`, Uint8Array.from(await page.screenshot()))
}

async function sleep(delay: number) {
    await new Promise(resolve => setTimeout(resolve, delay))
}

async function runServer(page: Page, port: number = 3000) {
    const app = express();

    app.use(express.static("."));
    app.get('/op/click', async (req, res) => {
        console.log(`query=${req.query.x}`)
        const x = parseInt(req.query.x as string)
        const y = parseInt(req.query.y as string)
        console.log(`Clicked(${x},${y})`)
        await page.mouse.click(x, y)
        await capture(page)
        // await fs.writeFileSync(`image.png`, Uint8Array.from(await page.screenshot()))
        res.send(`{}`)
    });
    const server = app.listen(port, () => console.log(`Server listening at http://localhost:${port} `));

    const wss = new WebSocket.Server({ server: server });
    wss.on('connection', function connection(ws) {
        ws.on('message', function incoming(message) {
            console.log('received: %s', message);
            ws.send('something');
        });
        ws.send('something');
    });

}

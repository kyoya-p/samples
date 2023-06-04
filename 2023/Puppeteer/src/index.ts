/*
headless Webブラウザを使用し、画像を連続的に保存するTSコードサンプル
*/

import fs from "fs";
import express from "express";
import puppeteer, { Page } from "puppeteer";
import { runServer2 } from "./server2";
import * as crypto from "crypto";

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
    runServer2(page, Number(process.argv[3] ?? "1000"))

    await page.authenticate({ username: process.env.USER ?? "", password: process.env.PASSWORD ?? "" });
    page.goto(process.argv[2] ?? "https://www.coolmathgames.com/ja/0-reversi");
    //setInterval(() => capture(page), 1000);

    // await browser.close();
}

let lastImgHash = crypto.createHash("sha256").update("").digest().toString("hex")
export async function capture2(page: Page) {
    const newImg = Buffer.from(await page.screenshot())
    const hash = crypto.createHash("sha256").update(newImg).digest().toString("hex")
    console.log(`image hash:${hash}`)
    if (lastImgHash !== hash) {
        fs.writeFileSync(`image.png`, newImg)
        lastImgHash = hash
        return hash
    }
    return null
}

// async function sleep(delay: number) {
//     await new Promise(resolve => setTimeout(resolve, delay))
// }

// async function runServer(page: Page, port: number = 3000) {
//     const app = express();
//     app.use(express.static("."));
//     app.get('/op/click', async (req: any, res: { send: (arg0: string) => void; }) => {
//         const x = parseInt(req.query.x)
//         const y = parseInt(req.query.y)
//         console.log(`Clicked(${x},${y})`)
//         await page.mouse.click(x, y)
//         await capture2(page)
//         console.log(`image updated.`)
//         res.send(`{}`)
//     });
//     app.listen(port, () => console.log(`Server listening at http://localhost:${port} `));
// }

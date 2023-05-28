/*
headless Webブラウザ上で画面が更新された場合に画像を連続的に抽出するTSのコードサンプルを

*/

import { puppeteer } from "puppeteer";

async function main() {
    const browser = await puppeteer.launch();
    const page = await browser.newPage();

    await page.goto("https://example.com");

    // 画面が更新されたときに画像をキャプチャする
    page.on("pageChanged", async () => {
        const image = await page.screenshot();
        // 画像を保存する
        await image.save("image.png");
    });

    // ブラウザを終了する
    await browser.close();
}
main();

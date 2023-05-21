import puppeteer from 'puppeteer';
import * as http from 'http';

async function main() {

    console.log(process.argv[2])
    const browser = await puppeteer.launch({
        headless: 'new',
        slowMo: 500,
    });
    const page = await browser.newPage();

    await page.goto('https://google.com');

    await page.screenshot({
        path: 'screenshot.png',
    });

    await browser.close();
}

main()

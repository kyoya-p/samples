import puppeteer from 'puppeteer';
// import * as http from 'http';

async function main() {

    console.log(process.argv[2])
    const browser = await puppeteer.launch({
        headless: 'new',
        slowMo: 500,
    });
    const page = await browser.newPage();

    await page.goto('https://google.com');

    await page.screenshot({ fromSurface: false, path: 'screenshot.~fromSurface.png', });
    await page.screenshot({ fullPage: true, path: 'screenshot.fullPage.png', });
    await page.screenshot({ quality: 1, path: 'screenshot.q1.jpg', });
    await page.screenshot({ quality: 2, path: 'screenshot.q2.jpg', });
    await page.screenshot({ quality: 3, path: 'screenshot.q3.jpg', });
    await page.screenshot({ quality: 5, path: 'screenshot.q5.jpg', });
    await page.screenshot({ quality: 10, path: 'screenshot.q10.jpg', });
    await page.screenshot({ quality: 25, path: 'screenshot.q25.jpg', });
    await page.screenshot({ quality: 50, path: 'screenshot.q50.jpg', });
    await page.screenshot({ quality: 75, path: 'screenshot.q75.jpg', });
    await page.screenshot({ quality: 100, path: 'screenshot.q100.jpg', });
    await page.screenshot({ quality: 0, path: 'screenshot.q0.jpg', });
    await page.screenshot({ fullPage: true, path: 'screenshot.fullPage.png', });
    await page.screenshot({ path: 'screenshot.png', });

    await browser.close();
}

main()

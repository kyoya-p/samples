const { chromium } = require('playwright');
(async () => {
    try {
        const browser = await chromium.launch({ 
            headless: false,
            args: [
                '--no-sandbox', 
                '--disable-setuid-sandbox',
                '--start-maximized'
            ]
        });
        // ビューポートをディスプレイサイズに合わせる
        const context = await browser.newContext({
            viewport: null
        });
        const page = await context.newPage();
        const url = process.env.TARGET_URL || 'http://server:3000/index.html';
        console.log('Navigating to ' + url);
        await page.goto(url);
        
        browser.on('disconnected', () => {
            console.log('Browser closed, exiting...');
            process.exit(0);
        });
    } catch (e) {
        console.error('Failed to launch browser:', e);
        process.exit(1);
    }
})();

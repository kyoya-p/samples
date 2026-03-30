const { chromium } = require('playwright');
(async () => {
    try {
        const browser = await chromium.launch({ 
            headless: false,
            timeout: 120000,
            args: [
                '--no-sandbox', 
                '--disable-setuid-sandbox',
                '--start-maximized',
                '--use-gl=angle',
                '--use-angle=swiftshader-webgl',
                '--disable-gpu-compositing'
            ]
        });
        const context = await browser.newContext({ viewport: { width: 1280, height: 1024 } });
        const page = await context.newPage();
        page.setDefaultNavigationTimeout(120000);
        const url = process.env.TARGET_URL || 'http://server:49880/index.html';
        console.log('Navigating to ' + url);
        await page.goto(url, { waitUntil: 'load', timeout: 120000 });
        
        console.log('Waiting for connect button...');
        await page.waitForSelector('#connectBtn', { timeout: 30000 });
        console.log('Clicking connect button...');
        await page.click('#connectBtn');

        // 定期的に状態をログ出力
        setInterval(async () => {
            try {
                const status = await page.evaluate(() => {
                    const dcStatus = document.getElementById('dcStatus').innerText;
                    const iceStatus = document.getElementById('connStatus').innerText;
                    const messages = Array.from(document.querySelectorAll('.message')).map(m => m.innerText);
                    return {
                        dcStatus,
                        iceStatus,
                        messageCount: messages.length,
                        lastMessage: messages[messages.length - 1] || null
                    };
                });
                console.log('CHECK_STATUS:' + JSON.stringify(status));

                // 接続が確立されていたらテストメッセージを送ってみる
                if (status.dcStatus.includes('open') && status.messageCount < 10) {
                    await page.fill('#chatInput', 'Hello from Playwright! ' + new Date().toLocaleTimeString());
                    await page.click('#sendBtn');
                }
            } catch (e) {
                console.error('Status check error:', e);
            }
        }, 5000);

        browser.on('disconnected', () => {
            console.log('Browser closed, exiting...');
            process.exit(0);
        });
    } catch (e) {
        console.error('Failed to launch browser:', e);
        process.exit(1);
    }
})();

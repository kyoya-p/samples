const { chromium } = require('playwright');
(async () => {
    try {
        const browser = await chromium.launch({ 
            headless: false,
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
        const url = process.env.TARGET_URL || 'http://server:49880/index.html';
        console.log('Navigating to ' + url);
        await page.goto(url);
        
        console.log('Waiting for connect button...');
        await page.waitForSelector('#connectBtn', { timeout: 10000 });
        console.log('Clicking connect button...');
        await page.click('#connectBtn');

        console.log('Waiting for localVideo...');
        await page.waitForSelector('#localVideo', { timeout: 10000 });
        console.log('Clicking localVideo to trigger animation...');
        await page.click('#localVideo');
        
        // 定期的にスクリーンショットを保存し、状態をログ出力
        setInterval(async () => {
            try {
                await page.screenshot({ path: '/app/remote-view.png' });
                const status = await page.evaluate(() => {
                    const v = document.getElementById('remoteVideo');
                    return {
                        srcObject: !!v.srcObject,
                        videoWidth: v.videoWidth,
                        videoHeight: v.videoHeight,
                        currentTime: v.currentTime.toFixed(2),
                        readyState: v.readyState
                    };
                });
                console.log('CHECK_REMOTE:' + JSON.stringify(status));
            } catch (e) {
                console.error('Screenshot/Status error:', e);
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

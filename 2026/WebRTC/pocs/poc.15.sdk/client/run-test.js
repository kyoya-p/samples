const { chromium } = require('playwright');

(async () => {
    const containerName = process.env.HOSTNAME || 'unknown';
    console.log(`[${containerName}] Starting SDK test via Playwright...`);

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

    try {
        const context = await browser.newContext({ viewport: null });
        const page = await context.newPage();

        page.on('console', msg => console.log(`[PAGE] ${msg.text()}`));

        const url = process.env.TARGET_URL || 'http://server:49880/client/index.html';
        console.log(`Navigating to ${url}`);
        await page.goto(url);

        // 60秒間、5秒おきにステータスをチェック
        for (let i = 0; i < 12; i++) {
            await new Promise(r => setTimeout(r, 5000));
            
            const status = await page.evaluate(() => {
                const iceEl = document.getElementById('iceStatus');
                const dcEl = document.getElementById('dcStatus');
                const peerEl = document.getElementById('peerCount');
                return {
                    ice: iceEl ? iceEl.innerText : 'N/A',
                    dc: dcEl ? dcEl.innerText : 'N/A',
                    peers: peerEl ? peerEl.innerText : '0'
                };
            });

            console.log(`CHECK_STATUS: ${JSON.stringify(status)}`);

            if (status.dc === 'open') {
                const msg = `Playwright message from ${containerName} at ${new Date().toLocaleTimeString()}`;
                await page.fill('#msgInput', msg);
                await page.click('#sendBtn');
                console.log(`SENT: ${msg}`);
            }
        }

        console.log('Test cycle finished.');
    } catch (e) {
        console.error('Test failed:', e);
    } finally {
        await browser.close();
    }
})();

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

        page.on('console', msg => {
            console.log('PAGE_LOG: ' + msg.text());
        });

        const url = process.env.TARGET_URL || 'http://server:49880/index.html';
        console.log('Navigating to ' + url);
        await page.goto(url);
        
        console.log('Waiting for connect button...');
        await page.waitForSelector('#connectBtn', { timeout: 10000 });
        console.log('Clicking connect button...');
        await page.click('#connectBtn');

        // 定期的に状態をログ出力
        setInterval(async () => {
            try {
                const status = await page.evaluate(() => {
                    const dcEl = document.getElementById('dcStatus');
                    const connEl = document.getElementById('connStatus');
                    if (!dcEl || !connEl) return { error: 'Elements not found' };

                    const dcStatus = dcEl.innerText;
                    const iceStatus = connEl.innerText;
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
                if (status.dcStatus && status.dcStatus.includes('open') && status.messageCount < 5) {
                    const inputEl = document.getElementById('chatInput');
                    if (inputEl && !inputEl.disabled) {
                        await new Promise(r => setTimeout(r, 500)); // 少し待機
                        const chatInput = document.querySelector('#chatInput');
                        const sendBtn = document.querySelector('#sendBtn');
                        if (chatInput && sendBtn) {
                           chatInput.value = 'Hello from Playwright! ' + new Date().toLocaleTimeString();
                           sendBtn.click();
                        }
                    }
                }
            } catch (e) {
                console.error('Status check error:', e.message);
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

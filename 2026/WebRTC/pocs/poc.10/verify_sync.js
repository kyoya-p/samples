const { chromium } = require('playwright');
const isHost = process.env.IS_HOST === 'true';

(async () => {
    const browser = await chromium.launch({ args: ['--no-sandbox'] });
    const page = await browser.newPage();
    const url = 'http://webrtc-full-stack-28002.japaneast.azurecontainer.io:3000/index.html';
    
    console.log(`[${isHost ? 'Host' : 'Guest'}] Navigating to URL...`);
    await page.goto(url);
    
    if (isHost) {
        console.log("[Host] Clicking Connect P2P to start negotiation...");
        await page.click('#btnConnect');
    } else {
        console.log("[Guest] Waiting for Host to initiate connection...");
    }
    
    console.log(`[${isHost ? 'Host' : 'Guest'}] Waiting for connection state: connected...`);
    await page.waitForFunction(() => document.getElementById('logArea').value.includes('WebRTC ConnectionState: connected'), { timeout: 30000 });
    console.log(`[${isHost ? 'Host' : 'Guest'}] Connection established!`);
    
    if (isHost) {
        console.log("[Host] Drawing on canvas...");
        const canvas = await page.$('#mainCanvas');
        const box = await canvas.boundingBox();
        await page.mouse.move(box.x + 50, box.y + 50);
        await page.mouse.down();
        await page.mouse.move(box.x + 150, box.y + 150);
        await page.mouse.up();
        await page.waitForTimeout(2000); // Give time for sync
        await page.screenshot({ path: '/app/host_sync.png' });
    } else {
        console.log("[Guest] Waiting for drawing to sync...");
        await page.waitForTimeout(4000); // Wait for host to draw
        await page.screenshot({ path: '/app/guest_sync.png' });
    }
    
    const log = await page.$eval('#logArea', el => el.value);
    console.log(`[${isHost ? 'Host' : 'Guest'}] Final Logs:\n`, log);
    
    await browser.close();
})();

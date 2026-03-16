const { test, expect } = require('@playwright/test');

const TARGET_URL = process.env.TARGET_URL || 'http://webrtc-full-stack-28002.japaneast.azurecontainer.io:3000/index.html';

test.describe('Azure WebRTC Service Verification', () => {
    test('P2P connection should reach connected state on Azure', async ({ browser }) => {
        const contextA = await browser.newContext();
        const contextB = await browser.newContext();
        
        const pageA = await contextA.newPage();
        const pageB = await contextB.newPage();

        console.log(`Navigating to ${TARGET_URL}`);
        await pageA.goto(TARGET_URL);
        await pageB.goto(TARGET_URL);

        // Connectボタンをクリック (Force TURN はデフォルトのオフのまま)
        await pageA.click('#btnConnect');

        // 両方のピアが connected 状態になるのを待機
        const checkConnected = async (page, name) => {
            console.log(`Waiting for ${name} to connect...`);
            await expect(page.locator('#logArea'), `${name} should connect`).toHaveValue(/(WebRTC ConnectionState: connected|WebRTC ICEConnectionState: connected)/, { timeout: 180000 });
            console.log(`${name} connected!`);
        };

        await Promise.all([
            checkConnected(pageA, 'Peer A'),
            checkConnected(pageB, 'Peer B')
        ]);

        // 現在日時を MMDD-HHMMSS 形式で取得
        const now = new Date();
        const prefix = String(now.getMonth() + 1).padStart(2, '0') + 
                       String(now.getDate()).padStart(2, '0') + '-' + 
                       String(now.getHours()).padStart(2, '0') + 
                       String(now.getMinutes()).padStart(2, '0') + 
                       String(now.getSeconds()).padStart(2, '0') + '_';

        // スクリーンショット保存
        await pageA.screenshot({ path: `.playwright-mcp/${prefix}azure_connected_peerA.png` });
        await pageB.screenshot({ path: `.playwright-mcp/${prefix}azure_connected_peerB.png` });

        // ピアAで描画
        console.log('Simulating drawing on Peer A...');
        const canvasA = pageA.locator('#mainCanvas');
        const boxA = await canvasA.boundingBox();
        await pageA.mouse.move(boxA.x + 100, boxA.y + 100);
        await pageA.mouse.down();
        await pageA.mouse.move(boxA.x + 200, boxA.y + 200);
        await pageA.mouse.up();

        // ピアBでの反映を待つ
        await pageB.waitForTimeout(3000);
        await pageB.screenshot({ path: `.playwright-mcp/${prefix}azure_sync_result_B.png` });

        await contextA.close();
        await contextB.close();
    });
});

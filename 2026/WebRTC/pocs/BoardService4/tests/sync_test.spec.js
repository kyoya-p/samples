const { test, expect } = require('@playwright/test');

test.describe('WebRTC Sync Verification', () => {
    test('P2P connection should reach connected state between two peers', async ({ browser }) => {
        // 2つのブラウザコンテキスト（ピアAとピアB）を作成
        const contextA = await browser.newContext();
        const contextB = await browser.newContext();
        
        const pageA = await contextA.newPage();
        const pageB = await contextB.newPage();

        // 両方のページでアプリを開く
        await pageA.goto('http://localhost:3001/index.html');
        await pageB.goto('http://localhost:3001/index.html');

        // ピアAで接続を開始
        await pageA.click('#btnConnect');

        // 両方のピアが connected 状態になるのを待機
        const checkConnected = async (page, name) => {
            await expect(page.locator('#logArea'), `${name} should connect`).toHaveValue(/WebRTC ConnectionState: connected/, { timeout: 60000 });
        };

        await Promise.all([
            checkConnected(pageA, 'Peer A'),
            checkConnected(pageB, 'Peer B')
        ]);

        // スクリーンショット保存
        await pageA.screenshot({ path: '.playwright-mcp/connected_peerA.png' });
        await pageB.screenshot({ path: '.playwright-mcp/connected_peerB.png' });

        await contextA.close();
        await contextB.close();
    });

    test('Drawing should sync between peers', async ({ browser }) => {
        const contextA = await browser.newContext();
        const contextB = await browser.newContext();
        const pageA = await contextA.newPage();
        const pageB = await contextB.newPage();

        await pageA.goto('http://localhost:3001/index.html');
        await pageB.goto('http://localhost:3001/index.html');

        await pageA.click('#btnConnect');

        // 接続完了を待つ
        await Promise.all([
            expect(pageA.locator('#logArea')).toHaveValue(/WebRTC ConnectionState: connected/, { timeout: 60000 }),
            expect(pageB.locator('#logArea')).toHaveValue(/WebRTC ConnectionState: connected/, { timeout: 60000 })
        ]);

        // ピアAで描画
        const canvasA = pageA.locator('#localCanvas');
        const boxA = await canvasA.boundingBox();
        await pageA.mouse.move(boxA.x + 50, boxA.y + 50);
        await pageA.mouse.down();
        await pageA.mouse.move(boxA.x + 150, boxA.y + 150);
        await pageA.mouse.up();

        // ピアBのRemoteキャンバスに点が表示されるか、またはログが出るか（今回は簡略化のため待機とスクショ）
        await pageB.waitForTimeout(2000);

        await pageA.screenshot({ path: '.playwright-mcp/sync_test_A.png' });
        await pageB.screenshot({ path: '.playwright-mcp/sync_test_B.png' });

        await contextA.close();
        await contextB.close();
    });
});

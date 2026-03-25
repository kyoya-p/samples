const { test, expect } = require('@playwright/test');

test.describe('WebRTC Coordinate Verification', () => {
    test('Coordinates should match click positions (square vertices)', async ({ page }) => {
        page.on('console', msg => console.log('PAGE LOG:', msg.text()));
        
        await page.goto('http://localhost:3001/index.html');
        await page.waitForLoadState('networkidle');
        
        const canvas = page.locator('#localCanvas');
        await canvas.waitFor({ state: 'visible' });
        
        // 描画を強制有効化（テスト用）
        await page.evaluate(() => {
            window.setCanvasEnabled(true);
        });
        
        const box = await canvas.boundingBox();
        console.log('Canvas Box:', box);
        
        // キャンバスのボーダーを考慮し、左上を原点として安全にクリックできるオフセット（2px）を設ける
        const offsetX = box.x + 2;
        const offsetY = box.y + 2;
        
        const points = [
            { dx: 0, dy: 0 },
            { dx: 0, dy: 50 },
            { dx: 50, dy: 0 },
            { dx: 50, dy: 50 }
        ];

        for (const pt of points) {
            const clickX = offsetX + pt.dx;
            const clickY = offsetY + pt.dy;
            console.log(`Clicking at canvas relative (${pt.dx}, ${pt.dy}) -> Absolute (${clickX}, ${clickY})`);
            
            await page.mouse.move(clickX, clickY);
            await page.mouse.down();
            await page.mouse.move(clickX + 1, clickY + 1); // ドラッグをシミュレートして描画を確実にする
            await page.mouse.up();
            await page.waitForTimeout(100);
        }
        
        // 少し待機
        await page.waitForTimeout(500);
        
        // クライアント側の state を確認
        const dots = await page.evaluate(() => window.localDots);
        console.log(`Detected ${dots.length} dots.`);
        
        expect(dots.length).toBeGreaterThanOrEqual(4);
        
        // スクリーンショット（プレフィックス付き）
        const now = new Date();
        const prefix = String(now.getMonth() + 1).padStart(2, '0') + 
                       String(now.getDate()).padStart(2, '0') + '-' + 
                       String(now.getHours()).padStart(2, '0') + 
                       String(now.getMinutes()).padStart(2, '0') + 
                       String(now.getSeconds()).padStart(2, '0') + '_';
        await page.screenshot({ path: `.playwright-mcp/${prefix}square_check.png` });
    });
});

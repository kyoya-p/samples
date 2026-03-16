#!/bin/bash
set -e

# 仮想ディスプレイの起動
export DISPLAY=:99
Xvfb :99 -screen 0 1280x1024x24 &
sleep 2

# ウィンドウマネージャの起動 (ウィンドウを正しく描画するために必要)
fluxbox &
sleep 2

# VNCサーバの起動
x11vnc -display :99 -forever -nopw -shared -rfbport 5900 &
sleep 2

# noVNCプロキシの起動 (6080ポート)
websockify --web /usr/share/novnc/ 6080 localhost:5900 &
sleep 2

# noVNCのURLを自動接続設定付きで表示
echo "Client started. Access noVNC at: http://localhost:<HOST_PORT>/vnc.html?autoconnect=true"

# Playwrightスクリプトの作成
cat <<EOF > /app/launch-browser.js
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
EOF

# Playwrightを使用してブラウザをヘッドフル（GUIあり）で起動
node /app/launch-browser.js

# プロセスを維持
wait

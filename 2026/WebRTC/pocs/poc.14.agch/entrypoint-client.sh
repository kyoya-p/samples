#!/bin/bash
# set -e removed to keep container alive on failure

# 仮想ディスプレイの起動
export DISPLAY=:99

# クリーンアップ: 古いロックファイルを削除
rm -f /tmp/.X99-lock /tmp/.X11-unix/X99

Xvfb :99 -screen 0 1280x1024x24 &
XVFBPID=$!

for i in {1..10}; do
    if xset -q -display :99 > /dev/null 2>&1; then
        echo "Xvfb is ready on :99"
        break
    fi
    echo "Waiting for Xvfb..."
    sleep 1
done

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

# Playwrightスクリプトの作成 (動的に生成)
cat <<EOF > /app/launch-browser.js
const { chromium } = require('playwright');
(async () => {
    try {
        console.log('Launching browser...');
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
        
        // タイムアウトを大幅に延長
        page.setDefaultNavigationTimeout(120000);
        page.setDefaultTimeout(120000);

        const url = process.env.TARGET_URL || 'http://host.docker.internal:49880/index.html';
        console.log('Navigating to ' + url + ' (timeout: 120s)');
        
        await page.goto(url, { waitUntil: 'load', timeout: 120000 });
        console.log('Page loaded successfully');
        
        // Auto-connect
        console.log('Waiting for connect button...');
        await page.waitForSelector('#connectBtn', { timeout: 60000 });
        console.log('Clicking connect button...');
        await page.click('#connectBtn');

        // Text chat logic for automation
        setInterval(async () => {
            try {
                const status = await page.evaluate(() => {
                    const dcStatus = document.getElementById('dcStatus');
                    const connStatus = document.getElementById('connStatus');
                    const dcText = dcStatus ? dcStatus.innerText : 'unknown';
                    const connText = connStatus ? connStatus.innerText : 'unknown';
                    const messages = Array.from(document.querySelectorAll('.message')).map(m => m.innerText);
                    return {
                        dcStatus: dcText,
                        connStatus: connText,
                        messageCount: messages.length
                    };
                });
                
                console.log('CHECK_STATUS:' + JSON.stringify(status));
                
                if (status.dcStatus.includes('open') && status.messageCount < 20) {
                    const msg = 'Automated message from ' + (process.env.HOSTNAME || 'client') + ' at ' + new Date().toLocaleTimeString();
                    await page.fill('#chatInput', msg);
                    await page.click('#sendBtn');
                }
            } catch (e) {
                // Ignore evaluation errors during transient states
            }
        }, 10000);
        
        browser.on('disconnected', () => {
            console.log('Browser closed, exiting...');
            process.exit(0);
        });
    } catch (e) {
        console.error('Failed in browser script:', e);
        process.exit(1);
    }
})();
EOF

echo "Starting launch-browser.js..."
node /app/launch-browser.js || echo "Browser launch script failed but keeping container alive..."

# プロセスを維持
tail -f /dev/null

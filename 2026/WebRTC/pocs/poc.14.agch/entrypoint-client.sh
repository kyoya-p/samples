#!/bin/bash
# set -e removed to keep container alive on failure

# 仮想ディスプレイの起動
export DISPLAY=:99

# クリーンアップ: 古いロックファイルを削除
rm -f /tmp/.X99-lock /tmp/.X11-unix/X99

Xvfb :99 -screen 0 1280x1024x24 &
XVFBPID=$!
sleep 2

# Xvfbの起動失敗をチェック
if ! ps -p $XVFBPID > /dev/null; then
    echo "Xvfb failed to start. Checking /tmp..."
    ls -la /tmp
    exit 1
fi

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
                '--start-maximized',
                '--use-gl=angle',
                '--use-angle=swiftshader-webgl',
                '--disable-gpu-compositing'
            ]
        });
        // ビューポートをディスプレイサイズに合わせる
        const context = await browser.newContext({
            viewport: null
        });
        const page = await context.newPage();

        page.on('console', msg => {
            console.log('PAGE_LOG: ' + msg.text());
        });

        const url = process.env.TARGET_URL || 'http://server:49880/index.html';
        console.log('Navigating to ' + url);
        await page.goto(url);
        
        // Auto-connect
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
                        const chatInput = document.querySelector('#chatInput');
                        const sendBtn = document.querySelector('#sendBtn');
                        if (chatInput && sendBtn) {
                           chatInput.value = 'Automated message at ' + new Date().toLocaleTimeString();
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
EOF

# Playwrightを使用してブラウザをヘッドフル（GUIあり）で起動
node /app/launch-browser.js || echo "Browser launch script failed but keeping container alive..."

# プロセスを維持
tail -f /dev/null

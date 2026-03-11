#!/bin/bash
set -e

# ロックファイルのクリーンアップ
rm -f /tmp/.X99-lock
rm -f /tmp/.X11-unix/X99

# 仮想ディスプレイの起動
export DISPLAY=:99
echo "Starting Xvfb on :99..."
Xvfb :99 -screen 0 1280x1024x24 > /tmp/xvfb.log 2>&1 &

# Xvfb起動待機
max_attempts=10
attempt=0
while ! xset -q >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    [ $attempt -ge $max_attempts ] && { echo "Xvfb start timeout"; exit 1; }
    echo "Waiting for Xvfb... ($attempt/$max_attempts)"
    sleep 1
done

# ウィンドウマネージャ起動
echo "Starting fluxbox..."
fluxbox > /tmp/fluxbox.log 2>&1 &
sleep 2

# VNCサーバ起動
echo "Starting x11vnc..."
x11vnc -display :99 -forever -nopw -shared -rfbport 5900 > /tmp/x11vnc.log 2>&1 &
sleep 2

# Websockify起動 (6081番ポートへ)
echo "Starting websockify on :6081..."
websockify --web /usr/share/novnc/ 6081 localhost:5900 > /tmp/websockify.log 2>&1 &
sleep 2

# URLトリガー付きランチャー & プロキシ
cat <<'EOF' > /app/url-launcher.js
const express = require('express');
const httpProxy = require('http-proxy');
const { chromium } = require('playwright');
const app = express();
const proxy = httpProxy.createProxyServer({ ws: true });
const PORT = 6080;
const WEBSOCKIFY_PORT = 6081;

let browser;
let page;

async function launchBrowser() {
    console.log('Launching Playwright browser...');
    browser = await chromium.launch({ 
        headless: false,
        args: ['--no-sandbox', '--disable-setuid-sandbox', '--start-maximized']
    });
    const context = await browser.newContext({ viewport: null });
    page = await context.newPage();
    const defaultUrl = process.env.TARGET_URL || 'http://server:3000/index.html';
    await page.goto(defaultUrl, { waitUntil: 'networkidle' });
}

app.all('*', async (req, res) => {
    // ?target=... が指定されている場合、ブラウザを遷移させる
    const targetUrl = req.query.target;
    if (targetUrl && page) {
        console.log(`URL Trigger detected: Navigating to ${targetUrl}`);
        await page.goto(targetUrl).catch(e => console.error('Goto failed:', e));
    }

    //  WebSocketアップグレードでなければ、通常の静的ファイルとしてWebsockifyへプロキシ
    if (req.headers.upgrade !== 'websocket') {
        proxy.web(req, res, { target: `http://localhost:${WEBSOCKIFY_PORT}` });
    }
});

const server = app.listen(PORT, async () => {
    console.log(`URL-Triggered Launcher Proxy running on port ${PORT}`);
    await launchBrowser();
});

server.on('upgrade', (req, socket, head) => {
    proxy.ws(req, socket, head, { target: `ws://localhost:${WEBSOCKIFY_PORT}` });
});
EOF

echo "Starting URL-triggered launcher..."
node /app/url-launcher.js &

# プロセスを維持し、ログを表示
tail -f /tmp/*.log

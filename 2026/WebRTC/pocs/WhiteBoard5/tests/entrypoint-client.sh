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

# Websockify起動
echo "Starting websockify on :6080..."
websockify --web /usr/share/novnc/ 6080 localhost:5900 > /tmp/websockify.log 2>&1 &
sleep 2

# Browserを起動
playwright codegen $TARGET_URL

# プロセスを維持し、ログを表示
tail -f /tmp/*.log

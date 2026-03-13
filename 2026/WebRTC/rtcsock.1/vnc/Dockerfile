FROM mcr.microsoft.com/playwright:v1.42.0-jammy

# インタラクティブなプロンプトを無効化
ENV DEBIAN_FRONTEND=noninteractive

# 必要なツールのインストール
RUN apt-get update && apt-get install -y \
    xvfb \
    x11vnc \
    websockify \
    novnc \
    fluxbox \
    xterm \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

RUN npm install playwright@1.58.2 && npx playwright install chromium

COPY launch_agent.js ./

# エントリポイントスクリプトの作成
RUN echo '#!/bin/bash\n\
rm -f /tmp/.X99-lock\n\
Xvfb :99 -screen 0 1280x1024x24 & \n\
sleep 2\n\
fluxbox & \n\
xterm -geometry 80x24+10+10 -e "watch -n 1 date" & \n\
x11vnc -display :99 -forever -nopw -shared -rfbport 5900 & \n\
# novnc の場所を修正し、静的ファイル配信を有効化\n\
websockify --web /usr/share/novnc/ 6080 localhost:5900 & \n\
sleep 5\n\
# エージェントブラウザを起動。シグナリングサーバへの接続先を環境変数から取得\n\
node launch_agent.js\n\
wait' > /entrypoint.sh && chmod +x /entrypoint.sh

EXPOSE 6080 5900

ENTRYPOINT ["/entrypoint.sh"]

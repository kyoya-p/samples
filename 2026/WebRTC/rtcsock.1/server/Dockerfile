FROM node:20-slim
WORKDIR /app
# ルートの package.json をコピーしてインストール
COPY package*.json ./
RUN npm install
# server ディレクトリごとコピー
COPY server/ ./server/
# 実行ディレクトリを server に変更
WORKDIR /app/server
EXPOSE 3000
CMD ["node", "server.js"]



# Build/Run
```
npm i
npx ts-node src/index.ts $targetUrl $servicePort
```

proxyの場合
```
export PROXY="http://proxy-server:port"
export USER="username"
export PASSWORD="password"
npx ts-node src/index.ts $targetUrl $servicePort
```

# Project生成
```:Typescript Project生成
npm init -y
npm i typescript @types/node --save-dev
npx tsc --init --rootDir src --outDir build

npm init playwright@latest

Getting started with writing end-to-end tests with Playwright:
Initializing project in '.'
✔ Where to put your end-to-end tests? · tests
✔ Add a GitHub Actions workflow? (y/N) · false
✔ Install Playwright browsers (can be done manually via 'npx playwright install')? (Y/n) · true
✔ Install Playwright operating system dependencies (requires sudo / root - can be done manually via 'sudo npx playwright install-deps')? (y/N) · false

npm i playwright
```


# Trouble Shooting
### 日本語フォントが化ける
```
sudo apt -y install locales fonts-ipafont fonts-ipaexfont
```


### 要求ライブラリのインストール
---
``bash
sudo apt-get install -y libgbm-dev
sudo apt-get install -y libatk-bridge2.0-0 
sudo apt-get install -y libgtk-3-0
sudo apt-get install -y libnss3 libx11-xcb1 libasound2
```
```terminal:実行時下記エラーが表示される場合
Error: Failed to launch the browser process!
/home/user/.cache/puppeteer/chrome/linux-113.0.5672.63/chrome-linux64/chrome: error while loading shared libraries: libgbm.so.1: cannot open shared object file: No such file or directory
```

参照
---
https://playwright.dev/

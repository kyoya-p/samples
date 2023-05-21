


Run
---
```
npx ts-node src/index.ts
```

Project生成
---

```:Typescript Project生成
npm init -y
npm i typescript @types/node --save-dev
npx tsc --init --rootDir src --outDir build

npm i puppeteer @types/puppeteer
```


Trouble Shooting
---

```:エラーメッセージ
Error: Failed to launch the browser process!
/home/user/.cache/puppeteer/chrome/linux-113.0.5672.63/chrome-linux64/chrome: error while loading shared libraries: libgbm.so.1: cannot open shared object file: No such file or directory
```

``bash
sudo apt-get install libgbm-dev
```

参照
---

https://github.com/puppeteer/puppeteer

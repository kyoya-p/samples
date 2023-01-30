see https://learn.microsoft.com/ja-jp/azure/azure-web-pubsub/tutorial-pub-sub-messages?tabs=javascript


```:Project作成
npm init -y
npm install typescript  @types/node --save-dev
npx tsc --init --rootDir src --outDir build --esModuleInterop --resolveJsonModule --lib es6,dom --module commonjs
```

```:ライブラリ
npm i @azure/web-pubsub ws
```




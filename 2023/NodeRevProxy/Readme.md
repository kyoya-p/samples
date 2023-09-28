```sh:Typescript Project生成
npm init -y
npm install typescript  @types/node --save-dev
npx tsc --init --rootDir src --outDir build --esModuleInterop --resolveJsonModule --lib es6,dom --module commonjs
```

```sh:ライブラリ
npm i http-proxy
npm i @novnc/novnc
```

```sh:Run
npx ts-node src/index.ts
```
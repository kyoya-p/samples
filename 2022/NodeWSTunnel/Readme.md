```:Project生成
npm init -y
npm install typescript --save-dev @types/node 
npx tsc --init --rootDir src --outDir lib --esModuleInterop --resolveJsonModule --lib es6,dom --module commonjs
```
import express from "express";
import httpProxy from "http-proxy";

const app = express();

// リバースプロキシの設定
const proxy = httpProxy.createProxyServer({
  target: "https://google.com",
  // changeOrigin: true,
});

proxy.listen(8080)
console.log(`start server port:8080.`)
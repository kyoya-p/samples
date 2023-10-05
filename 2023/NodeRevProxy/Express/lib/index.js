"use strict";
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
const express_1 = __importDefault(require("express"));
const http_proxy_1 = __importDefault(require("http-proxy"));
const app = (0, express_1.default)();
// リバースプロキシの設定
const proxy = http_proxy_1.default.createProxyServer({
    target: "http://localhost:8080",
    changeOrigin: true,
});
proxy.listen(8080);

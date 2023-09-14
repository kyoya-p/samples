import express from "express";
import { Context, HttpRequest } from "@azure/functions";

const app = express();

app.get("/", async (req: HttpRequest, context: Context) => {
  // Cookie を読み取る
  const cookies = req.headers.cookies;

  // 応答を返す
  return context.res.json({
    cookies,
  });
});

// Azure Functions にデプロイする
export default app;

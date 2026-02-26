import { GoogleGenerativeAI } from "@google/generative-ai";
import fs from "fs";
import dotenv from "dotenv";

dotenv.config();

const rawApiKey = process.env.GEMINI_API_KEY || "";
const apiKey = rawApiKey.trim();

if (!apiKey) {
  console.error("Error: GEMINI_API_KEY is not set.");
  process.exit(1);
}

const genAI = new GoogleGenerativeAI(apiKey);

async function analyzeImage(imagePath, customPrompt) {
  try {
    const model = genAI.getGenerativeModel({ model: "models/gemini-2.5-flash" });

    const prompt = customPrompt || `
画像を解析し、操作可能なすべてのUI要素（ボタン、入力欄、リンク、アイコン等）を特定してください。
各要素について、以下の情報をJSON形式で出力してください：
1. "element": 要素の名称（日本語）
2. "location": [ymin, xmin, ymax, xmax] の形式で正規化された座標（0-1000の範囲）
3. "purpose": その要素の役割や機能の説明
4. "suggested_action": この要素に対してエージェントが次に行うべき操作（click, type, scroll等）

また、画面全体の状況に基づく「現在のステータス」と「推奨される次の一手」も日本語で含めてください。`;

    const imageData = fs.readFileSync(imagePath);
    const imageParts = [
      {
        inlineData: {
          data: imageData.toString("base64"),
          mimeType: "image/png",
        },
      },
    ];

    console.log(`--- Analyzing Image (Agentic Vision): ${imagePath} ---`);
    
    const result = await model.generateContent({
      contents: [{ role: "user", parts: [{ text: prompt }, ...imageParts] }],
      generationConfig: {
        responseMimeType: "application/json",
      }
    });

    const response = await result.response;
    const text = response.text();
    
    console.log("\n--- Agentic Analysis Result (JSON) ---");
    try {
      const jsonResponse = JSON.parse(text);
      console.log(JSON.stringify(jsonResponse, null, 2));
    } catch (e) {
      console.log(text); // JSONパースに失敗した場合はそのまま表示
    }
  } catch (error) {
    console.error("Error during analysis:", error);
  }
}

const args = process.argv.slice(2);
const imagePath = args[0] || "current_desktop.png";
const prompt = args[1];

if (!fs.existsSync(imagePath)) {
  console.error(`Error: File not found: ${imagePath}`);
  process.exit(1);
}

analyzeImage(imagePath, prompt);

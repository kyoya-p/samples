import { GoogleGenerativeAI } from "@google/generative-ai";
import dotenv from "dotenv";
dotenv.config();

const genAI = new GoogleGenerativeAI(process.env.GOOGLE_API_KEY);

async function listModels() {
  const result = await genAI.listModels();
  for (const model of result.models) {
    console.log(`${model.name} (${model.supportedGenerationMethods})`);
  }
}

listModels().catch(console.error);

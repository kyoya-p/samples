import { GoogleGenerativeAI } from "@google/generative-ai";
import dotenv from "dotenv";

dotenv.config();

const apiKey = process.env.GEMINI_API_KEY?.trim();
if (!apiKey) {
  console.error("Error: GEMINI_API_KEY is not set.");
  process.exit(1);
}

const genAI = new GoogleGenerativeAI(apiKey);

async function listModels() {
  try {
    const response = await fetch(`https://generativelanguage.googleapis.com/v1beta/models?key=${apiKey}`);
    const data = await response.json();
    console.log("Available Models:");
    data.models?.forEach(m => console.log(`- ${m.name} (${m.displayName})`));
  } catch (error) {
    console.error("Error fetching models:", error);
  }
}

listModels();

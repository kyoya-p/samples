import { GoogleGenerativeAI } from "@google/generative-ai";

const apiKey = process.env.GOOGLE_API_KEY;
if (!apiKey) {
  console.error("GOOGLE_API_KEY is missing");
  process.exit(1);
}

const genAI = new GoogleGenerativeAI(apiKey);

async function listModels() {
  try {
    // Note: The newer SDK might have changed how this works
    const result = await genAI.getGenerativeModel({ model: "gemini-2.5-flash" }).generateContent("ping");
    console.log("Success with gemini-2.5-flash");
  } catch (e) {
    console.error(`Error: ${e.message}`);
  }
}

listModels();

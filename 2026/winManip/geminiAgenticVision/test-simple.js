import { GoogleGenerativeAI } from "@google/generative-ai";
import dotenv from "dotenv";
dotenv.config();

const genAI = new GoogleGenerativeAI(process.env.GOOGLE_API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });

async function test() {
  const result = await model.generateContent("Hello, are you there?");
  const response = await result.response;
  console.log(response.text());
}

test().catch(e => console.error(e.message));

#!/usr/bin/env node
import { GoogleGenerativeAI } from "@google/generative-ai";
import { Command } from "commander";
import chalk from "chalk";
import dotenv from "dotenv";
import fs from "fs/promises";
import path from "path";

dotenv.config();

const program = new Command();

program
  .name("vision-compare")
  .description("Compare image interpretation capabilities across Gemini models")
  .version("1.0.0")
  .argument("<image-path>", "Path to the image file")
  .option("-m, --models <models...>", "Models to compare", ["gemini-2.5-flash", "gemini-2.5-pro"])
  .option("-p, --prompt <prompt>", "Prompt for the models", "Describe this image in detail.")
  .action(async (imagePath, options) => {
    const apiKey = process.env.GOOGLE_API_KEY ?? process.env.GEMINI_API_KEY;
    if (!apiKey) {
      console.error(chalk.red("Error: GOOGLE_API_KEY (or GEMINI_API_KEY) is not set."));
      process.exit(1);
    }

    try {
      const imageData = await fs.readFile(imagePath);
      const mimeType = getMimeType(imagePath);
      const genAI = new GoogleGenerativeAI(apiKey);

      console.log(chalk.cyan(`\nImage: ${path.basename(imagePath)}`));
      console.log(chalk.cyan(`Prompt: ${options.prompt}\n`));

      const tasks = options.models.map(async (modelName) => {
        const model = genAI.getGenerativeModel({ model: modelName });
        const start = Date.now();
        
        try {
          const result = await model.generateContent([
            options.prompt,
            {
              inlineData: {
                data: imageData.toString("base64"),
                mimeType,
              },
            },
          ]);
          const response = await result.response;
          const text = response.text();
          const duration = ((Date.now() - start) / 1000).toFixed(2);

          return { modelName, text, duration, error: null };
        } catch (error) {
          return { modelName, text: null, duration: null, error: error.message };
        }
      });

      const results = await Promise.all(tasks);

      results.forEach(({ modelName, text, duration, error }) => {
        console.log(chalk.yellow.bold(`--- Model: ${modelName} (${duration ? duration + "s" : "N/A"}) ---`));
        if (error) {
          console.error(chalk.red(`Error: ${error}`));
        } else {
          console.log(text);
        }
        console.log("\n");
      });

    } catch (error) {
      console.error(chalk.red(`Failed to read image: ${error.message}`));
      process.exit(1);
    }
  });

function getMimeType(filePath) {
  const ext = path.extname(filePath).toLowerCase();
  switch (ext) {
    case ".png": return "image/png";
    case ".jpg":
    case ".jpeg": return "image/jpeg";
    case ".webp": return "image/webp";
    case ".heic": return "image/heic";
    case ".heif": return "image/heif";
    default: return "image/jpeg";
  }
}

program.parse();

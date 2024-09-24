import { GoogleAIFileManager } from "@google/generative-ai/server"
import { argv } from "process";

async function main() {
  const file = argv[2]
  const key = process.env.GOOGLE_API_KEY ?? ""
  const fileManager = new GoogleAIFileManager(key)

  await fileManager.deleteFile(file);
  console.log(`Deleted ${file}`);
}

main()

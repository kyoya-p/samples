import { GoogleAIFileManager } from "@google/generative-ai/server"

async function main() {
  const key = process.env.GOOGLE_API_KEY ?? ""
  const fileManager = new GoogleAIFileManager(key)
  const list = await fileManager.listFiles()
  console.log(list)
}

main()

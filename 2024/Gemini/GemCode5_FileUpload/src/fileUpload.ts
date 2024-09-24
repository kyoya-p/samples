import { FileMetadata, GoogleAIFileManager } from "@google/generative-ai/server"
async function main() {
    const file = process.argv[2]
    const fileManager = new GoogleAIFileManager(process.env.GOOGLE_API_KEY ?? "")
    const metaData: FileMetadata = { mimeType: "image/jpeg", displayName: `Image file '${file}'` }
    const uploadResponse = await fileManager.uploadFile(file, metaData)
    console.log(uploadResponse)
}
main()

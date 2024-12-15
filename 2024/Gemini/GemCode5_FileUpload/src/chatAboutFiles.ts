import { GoogleGenerativeAI, Part, FileDataPart } from "@google/generative-ai"
async function main() {
    const url = process.argv[2]
    const genAI = new GoogleGenerativeAI(process.env.GOOGLE_API_KEY ?? "")
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" })
    const files: Array<Part> = [{ fileData: { fileUri: url, mimeType: "image/jpeg" } },]
    const chat = model.startChat()
    async function qa(q: string) {
        const a = await chat.sendMessage(q)
        console.log(`Q: ${q}`)
        console.log(`A: ${a.response.text()}`)
        console.log(a.response.usageMetadata)
    }
    await chat.sendMessage(files)
    await qa("画像を20文字で説明して")
    await qa("カード上の透かし文言は何")
    await qa("カード上の透かしの矩形領域を示して[ymin, xmin, ymax, xmax]")
}
main()

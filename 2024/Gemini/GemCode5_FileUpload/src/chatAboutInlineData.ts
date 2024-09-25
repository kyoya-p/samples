import { GoogleGenerativeAI, Part } from "@google/generative-ai"
import fs from "fs"
import sharp from "sharp"


async function main() {
    const genAI = new GoogleGenerativeAI(process.env.GOOGLE_API_KEY ?? "")
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-pro-latest" })
    function filePart(path: string, mimeType: string): Part { return { inlineData: { data: Buffer.from(fs.readFileSync(path)).toString("base64"), mimeType, } } }

    // const q = "カードには赤紫緑白黄青の6色がある。第3の画像の色とコスト、第2の画像のカード名は?"
    const q = "画像左半分(カード部分)で透かしの文言は? またその矩形領域を文字ごとに示せ[[xmin,ymin,xmax,ymax],...]"
    const imageParts = [filePart("2.jpg", "image/jpeng"), q,]
    const a = await model.generateContent(imageParts)
    console.log(`Q: ${q}\nA: ${a.response.text()}`)
    console.log(a.response.usageMetadata)

    await sharp("2.jpg").extract({ 10, 10, 10, 10}).toFile("21.jog")
}

main()

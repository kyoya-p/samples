import { GoogleGenerativeAI, Part } from "@google/generative-ai"
import fs from "fs"
import { Jimp } from "jimp"


async function main() {
    const genAI = new GoogleGenerativeAI(process.env.GOOGLE_API_KEY ?? "")
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash-latest" })
    function filePart(path: string, mimeType: string): Part { return { inlineData: { data: Buffer.from(fs.readFileSync(path)).toString("base64"), mimeType, } } }

    const q = "画像左半分(カード部分)で透かしの文言は? またその矩形領域を文字ごとに示せ[[xmin,ymin,xmax,ymax],...]"
    const imageParts = [filePart("2.jpg", "image/jpeg"), q,]
    const a = await model.generateContent(imageParts)
    console.log(`Q: ${q}\nA: ${a.response.text()}`)
    console.log(a.response.usageMetadata)

    const img = await Jimp.read("2.jpg")
    img.circle({ radius: 25, x: 25, y: 25 })
    await img.write("2.1.jpg")
}

main()

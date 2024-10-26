import { GoogleGenerativeAI, Part } from "@google/generative-ai"
import fs from "fs"
import { Jimp } from "jimp"


async function main() {
<<<<<<< HEAD
    const genAI = new GoogleGenerativeAI(process.env.GOOGLE_API_KEY ?? "")
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash-latest" })
    function filePart(path: string, mimeType: string): Part { return { inlineData: { data: Buffer.from(fs.readFileSync(path)).toString("base64"), mimeType, } } }

    const q = "画像左半分(カード部分)で透かしの文言は? またその矩形領域を文字ごとに示せ[[xmin,ymin,xmax,ymax],...]"
    const imageParts = [filePart("2.jpg", "image/jpeg"), q,]
=======
    const file = process.argv[2]
    const genAI = new GoogleGenerativeAI(process.env.GOOGLE_API_KEY ?? "")
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-pro-latest" })
    function filePart(path: string, mimeType: string): Part { return { inlineData: { data: Buffer.from(fs.readFileSync(path)).toString("base64"), mimeType, } } }

    // const q = "画像左半分(カード部分)で透かしの文言は? またその矩形領域を文字ごとに示せ[[xmin,ymin,xmax,ymax],...]"
    const q = "画像左上にはこのカードのコストが数字で記事術される。カード中央には左から、カードのカテゴリ(スピリット、ネクサス等)、カード名、カードの系統が記載される。 枠の色はカードの属性(赤、青、紫、)を示す。このカードについて教えて]"
    const imageParts = [filePart(file, "image/jpeg"), q,]
>>>>>>> 5c49b22717c7df284ba2a8c598308810bfad3fe5
    const a = await model.generateContent(imageParts)
    console.log(`Q: ${q}\nA: ${a.response.text()}`)
    console.log(a.response.usageMetadata)

<<<<<<< HEAD
    const img = await Jimp.read("2.jpg")
    img.circle({ radius: 25, x: 25, y: 25 })
    await img.write("2.1.jpg")
=======
    // TODO

    // const img = await Jimp.read("2.jpg")
    // img.circle({ radius: 25, x: 25, y: 25 })
    // await img.write("2.1.jpg")
>>>>>>> 5c49b22717c7df284ba2a8c598308810bfad3fe5
}

main()

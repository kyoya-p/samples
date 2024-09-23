import { GoogleGenerativeAI } from "@google/generative-ai"

async function main() {
    const key = process.env.GOOGLE_API_KEY ?? ""
    const genAI = new GoogleGenerativeAI(key)
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });
    async function qa(q: string) {
        const a = await model.generateContent(q)
        console.log(`Q: ${q}`)
        console.log(`A: ${a.response.text()}`)
    }

    await qa("三角、四角、五角形、の次は? 簡潔に")
    await qa("その次は? 簡潔に")
}

main()

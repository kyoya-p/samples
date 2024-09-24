import { GoogleGenerativeAI } from "@google/generative-ai"

async function main() {
    const key = process.env.GOOGLE_API_KEY ?? ""
    const genAI = new GoogleGenerativeAI(key)
    const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" })
    const chat = model.startChat()

    async function qa(q: string) {
        const a = await chat.sendMessage(q)
        console.log(`Q: ${q}`)
        console.log(`A: ${a.response.text()}`)
        console.log(a.response.usageMetadata)
    }
    
    await qa("三角、の次は? 簡潔に")
    await qa("その次は? 簡潔に")
    await qa("最後の答えの前は? 簡潔に")
}

main()

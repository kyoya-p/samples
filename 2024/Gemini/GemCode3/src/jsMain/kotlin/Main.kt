external fun require(module: String): dynamic

data class GenerativeModel(val model: String)
external class GoogleGenerativeAI {
    fun getGenerativeModel(model: GenerativeModel)
}

external val process: dynamic

fun main() {
    val key: String = process.env.GOOGLE_API_KEY
    println("KEY: $key")
    val generativeAI = require("@google/generative-ai")
    val genAi: GoogleGenerativeAI = js("new generativeAI.GoogleGenerativeAI(key)")
    genAi.getGenerativeModel(GenerativeModel(model = "gemini-1.5-flash"))

    /*
const genAI = new GoogleGenerativeAI(process.env.API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-1.5-flash" });

const prompt = "Write a story about a magic backpack.";

const result = await model.generateContent(prompt);
console.log(result.response.text());
 */


}


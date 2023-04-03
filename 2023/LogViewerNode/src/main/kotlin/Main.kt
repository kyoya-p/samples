import NodeJS.Process


external val process: Process

fun main() {
    logParse("""
        aaa
        bbb
        c d e
        
        ss
    """.trimIndent())

}

fun logParse(logs: String) {
    logs.split("\n").forEach { println("[$it]") }
}
package ai.koog.samples

import ai.koog.prompt.dsl.*
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.clients.google.GoogleModels
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.io.files.Path
import kotlinx.coroutines.runBlocking
import com.microsoft.playwright.*
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class AgentAction(
    val thought: String,
    val action: String, // "navigate", "move", "click", "double_click", "right_click", "type", "press_key", "wait", "complete"
    val x: Int? = null,
    val y: Int? = null,
    val text: String? = null,
    val key: String? = null,
    val seconds: Int? = null,
    val url: String? = null
)

object PlaywrightController : AutoCloseable {
    private var playwright: Playwright? = null
    private var browser: Browser? = null
    private var context: BrowserContext? = null
    private var page: Page? = null

    const val viewportWidth = 1280
    const val viewportHeight = 800

    fun init() {
        if (playwright != null) return

        playwright = Playwright.create()
        val launchOptions = BrowserType.LaunchOptions()
            .setHeadless(false) // ヘッドフルモードで起動し動作を見える化

        // システムプロパティまたは環境変数からプロキシ情報を取得
        var proxyHost = System.getProperty("http.proxyHost") ?: ""
        var proxyPort = System.getProperty("http.proxyPort") ?: ""

        if (proxyHost.isEmpty()) {
            val envHttpProxy = System.getenv("HTTP_PROXY") ?: System.getenv("http_proxy") ?: ""
            if (envHttpProxy.isNotEmpty()) {
                val regex = Regex("https?://([^:]+):(\\d+)")
                val match = regex.find(envHttpProxy)
                if (match != null) {
                    proxyHost = match.groupValues[1]
                    proxyPort = match.groupValues[2]
                }
            }
        }

        if (proxyHost.isNotEmpty() && proxyPort.isNotEmpty()) {
            launchOptions.setProxy("http://$proxyHost:$proxyPort")
            println("[Playwright] Configured proxy: http://$proxyHost:$proxyPort")
        }

        browser = playwright!!.chromium().launch(launchOptions)
        context = browser!!.newContext(
            Browser.NewContextOptions()
                .setViewportSize(viewportWidth, viewportHeight)
                .setIgnoreHTTPSErrors(true) // SSLインスペクション環境対応 (エラー無視)
        )
        page = context!!.newPage()
        println("[Playwright] Initialized browser with viewport ${viewportWidth}x${viewportHeight}")
    }

    fun captureScreenshot(outputPath: String): String {
        init()
        val path = java.nio.file.Paths.get(outputPath)
        page!!.screenshot(Page.ScreenshotOptions().setPath(path))
        return path.toAbsolutePath().toString()
    }

    fun execute(action: AgentAction) {
        init()
        val p = page!!
        when (action.action.lowercase()) {
            "navigate", "goto" -> {
                val destinationUrl = action.url ?: action.text ?: return
                println("[Playwright] Navigating to: $destinationUrl")
                p.navigate(destinationUrl)
            }
            "move" -> {
                val tx = action.x?.toDouble() ?: return
                val ty = action.y?.toDouble() ?: return
                p.mouse().move(tx, ty)
                println("[Playwright] Moved mouse to ($tx, $ty)")
            }
            "click" -> {
                val tx = action.x?.toDouble() ?: return
                val ty = action.y?.toDouble() ?: return
                p.mouse().click(tx, ty)
                println("[Playwright] Clicked at ($tx, $ty)")
            }
            "double_click" -> {
                val tx = action.x?.toDouble() ?: return
                val ty = action.y?.toDouble() ?: return
                p.mouse().dblclick(tx, ty)
                println("[Playwright] Double-clicked at ($tx, $ty)")
            }
            "right_click" -> {
                val tx = action.x?.toDouble() ?: return
                val ty = action.y?.toDouble() ?: return
                p.mouse().click(tx, ty, com.microsoft.playwright.Mouse.ClickOptions().setButton(com.microsoft.playwright.options.MouseButton.RIGHT))
                println("[Playwright] Right-clicked at ($tx, $ty)")
            }
            "type" -> {
                val textToType = action.text ?: return
                println("[Playwright] Typing: '$textToType'")
                p.keyboard().type(textToType)
            }
            "press_key" -> {
                val rawKey = action.key ?: return
                val keyName = when(rawKey.uppercase()) {
                    "ENTER" -> "Enter"
                    "ESC", "ESCAPE" -> "Escape"
                    "TAB" -> "Tab"
                    "BACKSPACE", "BACK_SPACE" -> "Backspace"
                    "UP" -> "ArrowUp"
                    "DOWN" -> "ArrowDown"
                    "LEFT" -> "ArrowLeft"
                    "RIGHT" -> "ArrowRight"
                    else -> rawKey
                }
                println("[Playwright] Pressing key: $keyName")
                p.keyboard().press(keyName)
            }
            "wait" -> {
                val sec = action.seconds ?: 1
                println("[Playwright] Waiting for $sec second(s)...")
                p.waitForTimeout(sec * 1000.0)
            }
            "complete" -> {
                println("[Playwright] Action complete called.")
            }
            else -> {
                println("[Playwright] Unknown action: ${action.action}")
            }
        }
    }

    override fun close() {
        context?.close()
        browser?.close()
        playwright?.close()
        playwright = null
        browser = null
        context = null
        page = null
        println("[Playwright] Closed browser.")
    }
}

data class TestStep(
    val title: String,
    val instruction: String,
    val expected: String
)

data class TestProcedureContext(
    val metadata: String,
    val steps: List<TestStep>
)

class TestStepResult(
    val step: TestStep,
    var status: String, // "SUCCESS", "FAILURE", "TIMEOUT"
    var thought: String = "",
    var finalScreenshot: String = ""
)

fun parseTestProcedure(file: File): TestProcedureContext {
    val content = file.readText()
    val steps = mutableListOf<TestStep>()
    
    val firstStepIndex = content.indexOf("## Step")
    val metadata = if (firstStepIndex != -1) {
        content.substring(0, firstStepIndex).trim()
    } else {
        ""
    }
    
    val stepsContent = if (firstStepIndex != -1) {
        content.substring(firstStepIndex)
    } else {
        content
    }
    
    // Split content by ## Step headers
    val sections = stepsContent.split(Regex("(?=## Step)"))
    for (section in sections) {
        if (!section.trim().startsWith("## Step")) continue
        
        val lines = section.lines()
        val title = lines.firstOrNull { it.startsWith("## Step") }?.replace("## ", "")?.trim() ?: "Step"
        
        var instruction = ""
        var expected = ""
        
        for (line in lines) {
            if (line.trim().startsWith("- Action:") || line.trim().startsWith("- Instruction:")) {
                instruction = line.substringAfter(":").trim()
            }
            if (line.trim().startsWith("- Expected:")) {
                expected = line.substringAfter(":").trim()
            }
        }
        
        if (instruction.isEmpty()) {
            instruction = lines.drop(1).filter { it.trim().isNotEmpty() }.joinToString(" ") { it.trim().removePrefix("- ") }
        }
        
        steps.add(TestStep(title, instruction, expected))
    }
    
    if (steps.isEmpty()) {
        steps.add(TestStep("Overarching Goal", content.trim(), "Goal accomplished"))
    }
    
    return TestProcedureContext(metadata, steps)
}

fun main(args: Array<String>) = runBlocking {
    // JVM プロキシ設定の適用
    val npmProxy = System.getenv("npm_config_proxy") ?: ""
    val httpProxy = System.getenv("HTTP_PROXY") ?: System.getenv("http_proxy") ?: ""
    
    if (npmProxy.isNotEmpty()) {
        val proxyRegex = Regex("https?://([^:]+):([^@]+)@")
        val match = proxyRegex.find(npmProxy)
        if (match != null) {
            val user = match.groupValues[1]
            val pass = match.groupValues[2]
            java.net.Authenticator.setDefault(object : java.net.Authenticator() {
                override fun getPasswordAuthentication(): java.net.PasswordAuthentication? {
                    if (requestorType == RequestorType.PROXY) {
                        return java.net.PasswordAuthentication(user, pass.toCharArray())
                    }
                    return null
                }
            })
            println("[Proxy] Configured global proxy authenticator for user: $user")
        }
        
        val hostRegex = Regex("@([^:]+):(\\d+)")
        val hostMatch = hostRegex.find(npmProxy)
        if (hostMatch != null) {
            val host = hostMatch.groupValues[1]
            val port = hostMatch.groupValues[2]
            System.setProperty("http.proxyHost", host)
            System.setProperty("http.proxyPort", port)
            System.setProperty("https.proxyHost", host)
            System.setProperty("https.proxyPort", port)
            println("[Proxy] Configured JVM proxy properties to: $host:$port")
        }
    } else if (httpProxy.isNotEmpty()) {
        val hostRegex = Regex("https?://([^:]+):(\\d+)")
        val hostMatch = hostRegex.find(httpProxy)
        if (hostMatch != null) {
            val host = hostMatch.groupValues[1]
            val port = hostMatch.groupValues[2]
            System.setProperty("http.proxyHost", host)
            System.setProperty("http.proxyPort", port)
            System.setProperty("https.proxyHost", host)
            System.setProperty("https.proxyPort", port)
            println("[Proxy] Configured JVM proxy properties to: $host:$port")
        }
    }
    
    // SSLインスペクション対応のルートトラストストアを明示的に指定
    System.setProperty("javax.net.ssl.trustStore", "C:\\Users\\sharp\\dev\\.gemini\\skills\\sclan\\assets\\RootCA-proxy-nara.jks")
    System.setProperty("javax.net.ssl.trustStorePassword", "changeit")

    val firstArg = args.firstOrNull() ?: "Launch search, search for notepad, and open it."
    val isProcedureTest = firstArg.endsWith(".md") && File(firstArg).exists()

    // Gemini APIキーの検証
    val apiKey = System.getenv("GEMINI_API_KEY") ?: System.getenv("GOOGLE_API_KEY")
    if (apiKey.isNullOrEmpty()) {
        System.err.println("Error: Please set either GEMINI_API_KEY or GOOGLE_API_KEY environment variable.")
        return@runBlocking
    }
    val executor = simpleGoogleAIExecutor(apiKey)
    val jsonParser = Json { ignoreUnknownKeys = true }

    // スクリーンショット保存用一時ディレクトリ
    val tempDir = File("temp_screenshots")
    if (!tempDir.exists()) {
        tempDir.mkdirs()
    }

    PlaywrightController.use { playwrightController ->
        if (isProcedureTest) {
            val testFile = File(firstArg)
            println("==================================================")
            println(" JetBrains Koog + Playwright - Procedure Test Mode")
            println(" Input File: ${testFile.absolutePath}")
            println(" Viewport Size: ${PlaywrightController.viewportWidth}x${PlaywrightController.viewportHeight}")
            println("==================================================")

            val context = parseTestProcedure(testFile)
            val results = mutableListOf<TestStepResult>()
            var overallSuccess = true

            for ((index, step) in context.steps.withIndex()) {
                val stepNum = index + 1
                println("\n=== Executing Step $stepNum of ${context.steps.size}: ${step.title} ===")
                println("Instruction: ${step.instruction}")
                println("Expected: ${step.expected}")

                val stepResult = TestStepResult(step, "FAILURE")
                var stepCompleted = false
                var microStep = 1
                val maxMicroSteps = 5

                while (microStep <= maxMicroSteps && !stepCompleted) {
                    print("  [MicroStep $microStep/$maxMicroSteps] Capturing browser screen... ")
                    val screenshotPath = "temp_screenshots/step_${stepNum}_micro_$microStep.png"
                    PlaywrightController.captureScreenshot(screenshotPath)
                    stepResult.finalScreenshot = screenshotPath
                    println("Done.")

                    val promptObject = prompt("procedure-step-${stepNum}-micro-${microStep}") {
                        system("""
                            You are an automated GUI test execution assistant operating a Playwright-controlled browser on a Windows PC.
                            You are shown a screenshot of the browser's viewport and your task is to choose the single next action to execute towards completing the current step of the test procedure.
                            
                            Primary viewport dimensions: ${PlaywrightController.viewportWidth}x${PlaywrightController.viewportHeight} pixels.
                            
                            You MUST respond with a single, valid JSON block starting with '{' and ending with '}'.
                            Do NOT write any conversational text or explanation outside the JSON block.
                            
                            JSON Format:
                            {
                              "thought": "Analysis of the screenshot and the next action to take for this step.",
                              "action": "navigate|move|click|double_click|right_click|type|press_key|wait|complete",
                              "x": integer (x-coordinate, required for mouse actions. Must be within 0 to ${PlaywrightController.viewportWidth}),
                              "y": integer (y-coordinate, required for mouse actions. Must be within 0 to ${PlaywrightController.viewportHeight}),
                              "text": "string" (required for "type" action),
                              "key": "string" (required for "press_key" action: ENTER, ESC, TAB, BACKSPACE, UP, DOWN, LEFT, RIGHT),
                              "url": "string" (required for "navigate" action),
                              "seconds": integer (optional, for "wait" action)
                            }
                            
                            Instructions:
                            - Perform ONLY the next immediate action needed.
                            - If you need to open/go to a URL, use the "navigate" action with the "url" property.
                            - Do not try to type without clicking on the target input element first.
                            - When the current step's instruction and expected result have been successfully met, output the "complete" action.
                        """.trimIndent())
                        user {
                            text("""
                                Setup metadata / Context:
                                ${context.metadata}
                                
                                Current Step Title: ${step.title}
                                Instruction (Action to take): ${step.instruction}
                                Expected Result to verify: ${step.expected}
                            """.trimIndent())
                            image(Path(screenshotPath))
                        }
                    }

                    print("  Querying Gemini... ")
                    val response = executor.execute(promptObject, GoogleModels.Gemini2_5Flash)
                    val rawContent = response.textContent()
                    println("Done.")

                    val cleanedJson = extractJson(rawContent)
                    try {
                        val action = jsonParser.decodeFromString<AgentAction>(cleanedJson)
                        println("  Thought: ${action.thought}")
                        println("  Action: ${action.action.uppercase()}")
                        stepResult.thought = action.thought

                        if (action.action.lowercase() == "complete") {
                            stepCompleted = true
                            stepResult.status = "SUCCESS"
                            println("  [SUCCESS] Step completed successfully!")
                        } else {
                            PlaywrightController.execute(action)
                        }
                    } catch (e: Exception) {
                        System.err.println("  Failed to parse or execute: ${e.message}")
                    }

                    microStep++
                    Thread.sleep(1500)
                }

                if (!stepCompleted) {
                    stepResult.status = "TIMEOUT"
                    overallSuccess = false
                    println("  [TIMEOUT] Step failed to complete within $maxMicroSteps steps.")
                }
                results.add(stepResult)
            }

            // 出力レポートファイルの作成: test.md.out.YYMMDD-HHMM.md
            val now = LocalDateTime.now()
            val timestamp = now.format(DateTimeFormatter.ofPattern("yyMMdd-HHmm"))
            val outFileName = "${testFile.name}.out.$timestamp.md"
            val outFile = File(testFile.parentFile, outFileName)

            println("\nGenerating test report: ${outFile.absolutePath}")
            
            val report = StringBuilder()
            report.append("# Test Execution Report: ${testFile.name}\n\n")
            report.append("- **Execution Date**: ${now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}\n")
            report.append("- **Overall Result**: ${if (overallSuccess) "SUCCESS" else "FAILURE"}\n\n")
            report.append("## Executed Steps\n\n")

            for ((index, res) in results.withIndex()) {
                val stepNum = index + 1
                report.append("### Step $stepNum: ${res.step.title}\n")
                report.append("- **Instruction**: ${res.step.instruction}\n")
                report.append("- **Expected Result**: ${res.step.expected}\n")
                report.append("- **Status**: **${res.status}**\n")
                report.append("- **Last Thought**: ${res.thought}\n")
                report.append("- **Final Screenshot**: `${res.finalScreenshot}`\n\n")
            }

            outFile.writeText(report.toString())
            println("Report generated successfully.")
        } else {
            // 単一目標モード
            println("==================================================")
            println(" JetBrains Koog + Playwright - Goal Mode")
            println(" Target Goal: $firstArg")
            println(" Viewport Size: ${PlaywrightController.viewportWidth}x${PlaywrightController.viewportHeight}")
            println("==================================================")

            var currentStep = 1
            val maxSteps = 10
            var isCompleted = false

            while (currentStep <= maxSteps && !isCompleted) {
                println("\n--- Step $currentStep of $maxSteps ---")
                val screenshotPath = "temp_screenshots/step_$currentStep.png"
                PlaywrightController.captureScreenshot(screenshotPath)
                println("Captured screen: $screenshotPath")

                val promptObject = prompt("computer-use-turn-$currentStep") {
                    system("""
                        You are an automated GUI assistant operating a Playwright-controlled browser on a Windows PC.
                        You are shown a screenshot of the browser's viewport and your task is to choose the single next action to execute towards achieving the user's goal.
                        
                        Primary viewport dimensions are ${PlaywrightController.viewportWidth}x${PlaywrightController.viewportHeight} pixels.
                        
                        You MUST respond with a single, valid JSON block. Do NOT write any conversational text or explanation outside the JSON block.
                        
                        JSON Format:
                        {
                          "thought": "A detailed explanation of what you see in the screenshot and what action you will take.",
                          "action": "navigate|move|click|double_click|right_click|type|press_key|wait|complete",
                          "x": integer (x-coordinate, required for mouse actions. Must be within 0 to ${PlaywrightController.viewportWidth}),
                          "y": integer (y-coordinate, required for mouse actions. Must be within 0 to ${PlaywrightController.viewportHeight}),
                          "text": "string" (required for "type" action),
                          "key": "string" (required for "press_key" action: ENTER, ESC, TAB, BACKSPACE, UP, DOWN, LEFT, RIGHT),
                          "url": "string" (required for "navigate" action),
                          "seconds": integer (optional, for "wait" action)
                        }
                        
                        Instructions:
                        - Output ONLY valid JSON starting with '{' and ending with '}'.
                        - If you need to open/go to a URL, use the "navigate" action with the "url" property.
                        - Do not try to type without clicking on the target input element first.
                        - When the user's goal has been fully achieved, output the "complete" action.
                    """.trimIndent())
                    user {
                        text("Goal: $firstArg")
                        image(Path(screenshotPath))
                    }
                }

                print("Querying Gemini via Koog... ")
                val response = executor.execute(promptObject, GoogleModels.Gemini2_5Flash)
                val rawContent = response.textContent()
                println("Done.")
                println("Raw LLM Output:\n$rawContent")

                val cleanedJson = extractJson(rawContent)
                try {
                    val action = jsonParser.decodeFromString<AgentAction>(cleanedJson)
                    println("Thought: ${action.thought}")
                    println("Action: ${action.action.uppercase()}")
                    
                    PlaywrightController.execute(action)
                    
                    if (action.action.lowercase() == "complete") {
                        isCompleted = true
                        println("\nGoal successfully accomplished!")
                    }
                } catch (e: Exception) {
                    System.err.println("Failed to parse or execute action: ${e.message}")
                }

                currentStep++
                Thread.sleep(1500)
            }
        }
    }
}

fun extractJson(text: String): String {
    val startIndex = text.indexOf("{")
    val endIndex = text.lastIndexOf("}")
    if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
        return text.substring(startIndex, endIndex + 1)
    }
    return text
}

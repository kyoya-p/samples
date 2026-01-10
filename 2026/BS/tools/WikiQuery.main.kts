#!/usr/bin/env kotlin
@file:DependsOn("com.microsoft.playwright:playwright:1.49.0")
@file:DependsOn("org.jsoup:jsoup:1.18.3")
@file:DependsOn("com.google.code.gson:gson:2.10.1")

import com.microsoft.playwright.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.File
import com.google.gson.GsonBuilder

val SEARCH_URL = "https://batspi.com/index.php?%E3%82%AB%E3%83%BC%E3%83%89%E6%A4%9C%E7%B4%A2"
val BASE_URL = "https://batspi.com/"

data class CardBasic(val id: String, val name: String, val url: String)

data class CardDetail(
    val name: String,
    val type: String, // S, U, B, N, M
    val attr: String,
    val family: String,
    val lvCost: List<String>,
    val effect: String
)

fun scrapeWikiSearch(page: Page, keitoName: String?, effectText: String?): List<CardBasic> {
    println("[Search] Accessing search page: $SEARCH_URL")
    page.navigate(SEARCH_URL)

    if (!keitoName.isNullOrEmpty()) {
        println("[Search] Filling system: $keitoName")
        page.fill("#KEITO_NAME", keitoName)
    }

    if (!effectText.isNullOrEmpty()) {
        println("[Search] Filling effect: $effectText")
        page.fill("#KOUKA", effectText)
    }

    println("[Search] Submitting search form...")
    page.click("input[name=\"Search\"]")

    println("[Search] Waiting for results page to load...")
    try {
        page.waitForSelector("#body table", Page.WaitForSelectorOptions().setTimeout(10000.0))
    } catch (e: Exception) {
        println("(!) No results found or timeout occurred.")
        return emptyList()
    }

    println("[Search] Parsing results...")
    val content = page.content()
    val doc = Jsoup.parse(content)
    val cards = mutableListOf<CardBasic>()

    val body = doc.getElementById("body")
    if (body != null) {
        val tables = body.getElementsByTag("table")
        val idRegex = Regex("([A-Z\\d]{2,}-\\d+[A-Z\\d]*)")
        for (table in tables) {
            val text = table.text()
            if (text.contains("カード番号") && text.contains("カード名")) {
                val links = table.select("a[href]")
                var cardId = ""
                var cardName = ""
                var cardUrl = ""

                val matchResult = idRegex.find(text)
                if (matchResult != null) {
                    cardId = matchResult.groupValues[1]
                }

                for (link in links) {
                    val href = link.attr("href")
                    if (href.contains("index.php?") &&
                        !href.contains("cmd=") &&
                        !href.contains("related=") &&
                        !href.contains("Search")
                    ) {
                        cardUrl = if (href.startsWith("http")) href else BASE_URL + href
                        cardName = link.text().trim()
                        if (cardName.isNotEmpty() && cardId.isNotEmpty()) break
                    }
                }

                if (cardId.isNotEmpty() && cardName.isNotEmpty()) {
                    cards.add(CardBasic(cardId, cardName, cardUrl))
                }
            }
        }
    }

    val uniqueCards = cards.associateBy { it.id }.values.toList()
    println("[Search] Found ${uniqueCards.size} unique cards.")
    return uniqueCards
}

fun scrapeCardDetail(page: Page, card: CardBasic): CardDetail? {
    println("[Detail] Scraping ${card.name} (${card.id})...")
    try {
        page.navigate(card.url)
        val content = page.content()
        val doc = Jsoup.parse(content)

        // Find the main table. Usually contains "カード名" and "種類"
        val tables = doc.select("table")
        var targetTable: Element? = null
        for (table in tables) {
            if (table.text().contains("カード名") && (table.text().contains("種類") || table.text().contains("属性"))) {
                targetTable = table
                break
            }
        }

        if (targetTable == null) {
            println("  (!) Could not find detail table for ${card.name}")
            return null
        }

        var type = ""
        var attr = ""
        var family = ""
        var effect = ""
        val lvCostList = mutableListOf<String>()

        // Helper to extract text from table rows
        val rows = targetTable.select("tr")
        for (row in rows) {
            val header = row.select("th").text().trim()
            val data = row.select("td").text().trim()

            if (header.contains("種類")) {
                type = when {
                    data.contains("スピリット") -> "S"
                    data.contains("アルティメット") -> "U"
                    data.contains("ブレイヴ") -> "B"
                    data.contains("ネクサス") -> "N"
                    data.contains("マジック") -> "M"
                    else -> "Unknown"
                }
            } else if (header.contains("属性")) {
                val colors = listOf("赤", "紫", "緑", "白", "黄", "青")
                val foundColors = colors.filter { data.contains(it) }
                attr = foundColors.joinToString("")
            } else if (header.contains("系統")) {
                family = data.replace(" ", "").replace("、", ",")
            } else if (header.contains("効果")) {
                effect = data
            }
        }
        
        // Try to find Lv/BP info. This is often in the same table or mixed.
        // Strategy: Look for text patterns like "Lv1 1 3000" in the whole table text or specific cells
        // Since structure varies, we'll do a best effort regex search on the table text
        val tableText = targetTable.text()
        
        // Regex for Lv entries: LvX <cost> <bp>
        // Example: Lv1 1 3000
        // Sometimes: Lv1 1-Lv2 3-Lv3 4
        // Or in separate cells.
        
        // Let's try to find patterns like "Lv1 1 3000"
        // Pattern: Lv(\d) (\d+) (\d+)
        val lvRegex = Regex("Lv(\\d)\\s+(\\d+)\\s+(\\d+)")
        val matches = lvRegex.findAll(tableText)
        for (match in matches) {
            val lv = match.groupValues[1]
            val cost = match.groupValues[2]
            val bp = match.groupValues[3]
            lvCostList.add("$lv,$cost,$bp")
        }
        
        // If regex didn't work, maybe it's formatted differently.
        // For now, we stick to this. If empty and it's not magic, it might be an issue.
        
        return CardDetail(
            name = card.name,
            type = type,
            attr = attr,
            family = family,
            lvCost = lvCostList,
            effect = effect
        )

    } catch (e: Exception) {
        println("  (!) Error scraping detail for ${card.name}: ${e.message}")
        return null
    }
}

fun printHelp() {
    println("""
        Usage: WikiQuery.main.kts [options] [query]
        
        Options:
          -s, --system <name>   Search by system (系統)
          -e, --effect <text>   Search by effect text (効果)
          -h, --help            Show this help message
          
        Examples:
          WikiQuery.main.kts 超星
          WikiQuery.main.kts -s 星竜
          WikiQuery.main.kts -e バースト
    """.trimIndent())
}

fun main(args: Array<String>) {
    try {
        if (args.isEmpty()) {
            printHelp()
            return
        }

        var keito: String? = null
        var effect: String? = null
        var query: String? = null

        var i = 0
        while (i < args.size) {
            when (val arg = args[i]) {
                "-h", "--help" -> {
                    printHelp()
                    return
                }
                "-s", "--system" -> {
                    if (i + 1 < args.size) {
                        keito = args[i + 1]
                        i++
                    }
                }
                "-e", "--effect" -> {
                    if (i + 1 < args.size) {
                        effect = args[i + 1]
                        i++
                    }
                }
                else -> if (!arg.startsWith("-")) query = arg
            }
            i++
        }

        if (keito == null && query != null) keito = query
        
        if (keito == null && effect == null) {
            println("Error: No search criteria provided.")
            printHelp()
            return
        }

        println("Starting WikiQuery...")
        println("System: $keito, Effect: $effect")

        Playwright.create().use { playwright ->
            playwright.chromium().launch(BrowserType.LaunchOptions().setHeadless(false)).use { browser ->
                val page = browser.newPage()
                
                // 1. Search
                val searchResults = scrapeWikiSearch(page, keito, effect)
                
                if (searchResults.isEmpty()) {
                    println("No cards found.")
                    return
                }

                // 2. Prepare output directory
                val outputDir = File("card")
                if (!outputDir.exists()) {
                    outputDir.mkdirs()
                }

                val gson = GsonBuilder().setPrettyPrinting().create()

                // 3. Process each card
                for (cardBasic in searchResults) {
                    val detail = scrapeCardDetail(page, cardBasic)
                    if (detail != null) {
                        // Filename: card/カードID.カード名.タイプ.attr.family.json
                        // Sanitize filename
                        val safeName = detail.name.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                        val safeFamily = detail.family.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
                        
                        val fileName = "${cardBasic.id}.${safeName}.${detail.type}.${detail.attr}.${safeFamily}.json"
                        val file = File(outputDir, fileName)
                        
                        val json = gson.toJson(detail)
                        file.writeText(json)
                        println("  -> Saved to ${file.path}")
                    }
                    // Be nice to the server
                    Thread.sleep(500) 
                }
                
                page.close()
            }
        }

    } catch (t: Throwable) {
        println("An error occurred: ${t.message}")
        t.printStackTrace()
    } finally {
        println("Program finished.")
        Runtime.getRuntime().halt(0)
    }
}

main(args)

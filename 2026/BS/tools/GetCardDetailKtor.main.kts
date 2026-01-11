@file:Repository("https://repo1.maven.org/maven2/")
@file:DependsOn("io.ktor:ktor-client-core-jvm:3.0.3")
@file:DependsOn("io.ktor:ktor-client-cio-jvm:3.0.3")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.9.0")
@file:DependsOn("com.fleeksoft.ksoup:ksoup-jvm:0.2.0")

// Updated version with lambda fixes

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.*
import kotlinx.coroutines.runBlocking
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.nodes.Element

data class Card(
    val id: String,
    val sides: List<CardSide>
)

data class CardSide(
    val side: String, // "A" or "B"
    val name: String,
    val rarity: String,
    val cost: Int?,
    val reductionSymbols: List<String>,
    val attributes: List<String>,
    val category: String,
    val systems: List<String>,
    val lvInfo: List<LvInfo>,
    val effect: String,
    val imageUrl: String
)

data class LvInfo(val level: Int, val core: Int, val bp: Int)

val client = HttpClient(CIO) {
    install(HttpTimeout) {
        requestTimeoutMillis = 30000
    }
}

fun parseCardSide(root: Element, sideName: String): CardSide {
    // Basic Info
    val idRarity = root.select(".cardNum").text().trim()
    val rarity = root.select(".cardRarity").text().trim()
    val name = root.select(".cardName").text().trim()
    
    // Cost
    val costText = root.select(".textCost").text().trim()
    val cost = costText.toIntOrNull()
    val reductionSymbols = root.select(".cost img").map { it.attr("alt") }

    // Category
    val category = root.select("dt:contains(カテゴリー) + dd").text().trim()
    
    // Attributes
    val attributes = root.select(".attribute .attributeItem").map { it.text().trim() }
    
    // Systems
    val systems = root.select("dt:contains(系統) + dd").text().split("・").filter { it.isNotBlank() }

    // Lv Info
    val lvInfo = root.select(".bpCoreItem").mapNotNull { item ->
        val lvImg = item.select("img.bpCoreImg").attr("alt")
        if (lvImg.isEmpty()) return@mapNotNull null
        
        val lv = lvImg.replace("LV", "").toIntOrNull() ?: 0
        val bp = item.select(".bpCoreVal").text().trim().toIntOrNull() ?: 0
        val core = item.select(".bpCoreLevel").text().trim().toIntOrNull() ?: 0
        LvInfo(lv, core, bp)
    }

    // Effect
    val effectElement = root.select(".detailColListDescription.wide").first()?.clone()
    effectElement?.select("img")?.forEach { img ->
        val alt = img.attr("alt")
        img.after("[" + alt + "]")
        img.remove()
    }
    val effect = effectElement?.let { 
        val cleanHtml = it.html().replace("<br>", "\n").replace("<br />", "\n")
        Ksoup.parse(cleanHtml).text() 
    }?.trim() ?: ""

    val imageUrl = "https://www.battlespirits.com" + root.select(".cardImg img").attr("src")

    return CardSide(
        side = sideName,
        name = name,
        rarity = rarity,
        cost = cost,
        reductionSymbols = reductionSymbols,
        attributes = attributes,
        category = category,
        systems = systems,
        lvInfo = lvInfo,
        effect = effect,
        imageUrl = imageUrl
    )
}

fun parseCard(html: String): Card {
    val doc: Document = Ksoup.parse(html)
    val sides = mutableListOf<CardSide>()

    val sideA = doc.select("#CardCol_A").first()
    if (sideA != null) {
        sides.add(parseCardSide(sideA, "A"))
    }

    val sideB = doc.select("#CardCol_B").first()
    if (sideB != null) {
        sides.add(parseCardSide(sideB, "B"))
    }

    if (sides.isEmpty()) throw Exception("No card data found")

    // Extract ID from Side A (or B if A missing)
    val baseIdRarity = sides.first().let { 
        val root = if(it.side == "A") sideA!! else sideB!!
        root.select(".cardNum").text().trim() 
    }
    val baseRarity = sides.first().rarity
    val id = baseIdRarity.removeSuffix(baseRarity).trim()

    return Card(id, sides)
}

val targetCardNo = args.getOrNull(0) ?: "BS58-TCP04" 

runBlocking {
    try {
        println("Fetching details for: $targetCardNo")
        val response = client.get("https://www.battlespirits.com/cardlist/detail_iframe.php") {
            parameter("card_no", targetCardNo)
            parameter("card_no2", targetCardNo)
            header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/143.0.0.0 Safari/537.36")
        }

        if (response.status == HttpStatusCode.OK) {
            val card = parseCard(response.bodyAsText())
            
            println("\n=== Card ID: ${card.id} ===")
            
            card.sides.forEach { side ->
                println("\n--- Side ${side.side} ---")
                println("Name: ${side.name}")
                println("Rarity: ${side.rarity}")
                println("Cost: ${side.cost} (Reduction: ${side.reductionSymbols.joinToString("")})")
                println("Category: ${side.category}")
                println("Attributes: ${side.attributes.joinToString("/")}")
                println("Systems: ${side.systems.joinToString("/")}")
                
                if (side.lvInfo.isNotEmpty()) {
                    println("\n[Stats]")
                    side.lvInfo.forEach { println("Lv${it.level} (${it.core} core): ${it.bp} BP") }
                }
                
                println("\n[Effect]")
                println(side.effect)
                println("\n[Image]")
                println(side.imageUrl)
            }
        } else {
            println("Failed to fetch: ${response.status}")
        }
    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    } finally {
        client.close()
    }
}
package gamefield

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import java.io.File
import kotlinx.serialization.Serializable
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration

@Serializable
data class CardFace(
    val name: String = "",
    val cost: Int = 0,
    val symbols: String = "",
    val reductionSymbols: String = "",
    val category: String = ""
)

@Serializable
data class CardYaml(
    val sideA: CardFace
)

enum class Step(val displayName: String) {
    START("Start Step"),
    CORE("Core Step"),
    DRAW("Draw Step"),
    REFRESH("Refresh Step"),
    MAIN("Main Step"),
    ATTACK("Attack Step"),
    END("End Step"),
    OPPONENT_TURN("Opponent Turn")
}

class GameState {
    var currentStep = Step.START
    var isFirstPlayer = true
    var turnCount = 1
    
    val deck = mutableListOf<String>()
    val hand = mutableListOf<String>()
    val field = mutableListOf<String>()
    var reserve = 3
    var hasSoulCoreInReserve = true
    var life = 5
    var coreTrash = 0

    private val cardCache = mutableMapOf<String, CardData>()
    private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

    data class CardData(val name: String, val cost: Int, val reduction: Int, val symbols: Int)

    fun getCardData(name: String): CardData {
        return cardCache[name] ?: CardData(name, 3, 1, 1)
    }

    private fun loadCardData(cardNo: String, name: String) {
        val userHome = System.getProperty("user.home")
        val yamlFile = File("$userHome/.bscards/yaml/$cardNo.yaml")
        if (yamlFile.exists()) {
            try {
                val data = yaml.decodeFromString(CardYaml.serializer(), yamlFile.readText())
                val face = data.sideA
                
                val reduction = if (face.reductionSymbols.contains("全")) {
                    face.reductionSymbols.filter { char -> char.isDigit() }.toIntOrNull() ?: 0
                } else {
                    val digits = face.reductionSymbols.filter { char -> char.isDigit() }
                    if (digits.isEmpty() && face.reductionSymbols.isNotEmpty()) 1 
                    else digits.map { char -> char.toString().toInt() }.sum()
                }
                
                // シンボル計算: 契約神ネクサスやスピリットは基本1シンボル
                var symbolCount = face.symbols.filter { char -> char.isDigit() }.map { char -> char.toString().toInt() }.sum()
                if (symbolCount == 0) {
                    // シンボル欄が空でも、スピリット、アルティメット、ネクサス、ブレイヴは基本1シンボル持つ
                    if (face.category.contains("スピリット") || face.category.contains("ネクサス") || 
                        face.category.contains("アルティメット") || face.category.contains("ブレイヴ")) {
                        symbolCount = 1
                    }
                }
                
                cardCache[name] = CardData(name, face.cost, reduction, symbolCount)
            } catch (e: Exception) {
                // Fallback
            }
        }
    }

    fun getFieldSymbols(): Int {
        return field.sumOf { name -> getCardData(name).symbols }
    }

    fun calculateEffectiveCost(name: String): Int {
        val data = getCardData(name)
        val symbols = getFieldSymbols()
        return maxOf(data.cost - minOf(data.reduction, symbols), 0)
    }

    fun setup() {
        val potentialPaths = listOf(
            "BS/data/decks/indra_raimu.md",
            "../data/decks/indra_raimu.md",
            "../../BS/data/decks/indra_raimu.md",
            "C:/Users/kyoya/works/samples/2026/BS/data/decks/indra_raimu.md"
        )
        val deckFile = potentialPaths.map { File(it) }.find { it.exists() } ?: return

        val cards = mutableListOf<Pair<String, String>>()
        deckFile.readLines().forEach { line ->
            if (line.trim().startsWith("|") && !line.contains("---") && !line.contains("カテゴリ")) {
                val parts = line.split("|").map { it.trim() }
                if (parts.size >= 5) {
                    val count = parts[2].toIntOrNull() ?: 0
                    val no = parts[3]
                    val name = parts[4]
                    if (name.isNotBlank()) {
                        loadCardData(no, name)
                        repeat(count) { cards.add(no to name) }
                    }
                }
            }
        }
        
        deck.clear()
        deck.addAll(cards.map { it.second })
        
        val contract = deck.find { it.contains("インドラ") && !it.contains("天雷神殿") } 
        if (contract != null) {
            deck.remove(contract)
            hand.add(contract)
        }
        
        deck.shuffle()
        repeat(3) { if (deck.isNotEmpty()) hand.add(deck.removeAt(0)) }
        
        reserve = 3
        hasSoulCoreInReserve = true
        coreTrash = 0
    }

    fun printStatus() {
        val totalCores = reserve + (if (hasSoulCoreInReserve) 1 else 0)
        val symbols = getFieldSymbols()
        println("\n===============================")
        println("  ${currentStep.displayName} (Turn $turnCount)")
        println("===============================")
        println("Life: $life, Total Reserve: $totalCores (Normal: $reserve, Soul: ${if (hasSoulCoreInReserve) "1" else "0"})")
        println("Symbols: $symbols, CoreTrash: $coreTrash, Deck: ${deck.size}")
        println("Field: ${if (field.isEmpty()) "None" else field.joinToString(", ")}")
        println("Hand:  ${if (hand.isEmpty()) "Empty" else hand.joinToString(", ")}")
        println("-------------------------------")
    }

    fun autoProgress() {
        while (currentStep != Step.MAIN && currentStep != Step.ATTACK && currentStep != Step.OPPONENT_TURN) {
            printStatus()
            when (currentStep) {
                Step.START -> currentStep = Step.CORE
                Step.CORE -> {
                    if (!(isFirstPlayer && turnCount == 1)) {
                        reserve++
                        println(">> Core Step: Gained 1 core.")
                    } else {
                        println(">> Core Step: Skipped (1st turn).")
                    }
                    currentStep = Step.DRAW
                }
                Step.DRAW -> {
                    if (!(isFirstPlayer && turnCount == 1)) {
                        if (deck.isNotEmpty()) {
                            val card = deck.removeAt(0)
                            hand.add(card)
                            println(">> Draw Step: Drew [$card].")
                        }
                    } else {
                        println(">> Draw Step: Skipped (1st turn).")
                    }
                    currentStep = Step.REFRESH
                }
                Step.REFRESH -> {
                    if (coreTrash > 0) {
                        println(">> Refresh Step: Moving $coreTrash cores back to reserve.")
                        reserve += coreTrash
                        coreTrash = 0
                    }
                    if (!hasSoulCoreInReserve) {
                         hasSoulCoreInReserve = true
                         println(">> Soul Core returned to reserve.")
                    }
                    currentStep = Step.MAIN
                }
                else -> {}
            }
            Thread.sleep(100)
        }
        if (currentStep != Step.OPPONENT_TURN) printStatus()
    }
}

class Simulate : CliktCommand(name = "simulate") {
    override fun run() {
        val state = GameState()
        state.setup()
        println("=== Battle Spirits CLI Simulator ===")
        
        state.autoProgress()

        while (state.currentStep != Step.OPPONENT_TURN) {
            val choices = mutableListOf<String>()
            val actions = mutableListOf<() -> Unit>()

            when (state.currentStep) {
                Step.MAIN -> {
                    state.hand.forEachIndexed { indexInHand, name ->
                        val effectiveCost = state.calculateEffectiveCost(name)
                        choices.add("Summon [$name] (Cost: $effectiveCost)")
                        actions.add {
                            val totalAvailable = state.reserve + (if (state.hasSoulCoreInReserve) 1 else 0)
                            if (totalAvailable >= effectiveCost) {
                                payCost(state, effectiveCost)
                                state.hand.removeAt(indexInHand)
                                state.field.add(name)
                                println(">> Summoned: $name")
                            } else {
                                println(">> Not enough cores!")
                            }
                        }
                    }
                    choices.add("End Main Step")
                    actions.add { state.currentStep = Step.ATTACK }
                }
                Step.ATTACK -> {
                    state.field.forEachIndexed { i, name ->
                        choices.add("Attack with [$name]")
                        actions.add { println(">> Attacking with: $name!") }
                    }
                    choices.add("End Attack Step")
                    actions.add { state.currentStep = Step.END }
                }
                Step.END -> {
                    choices.add("Finish Turn")
                    actions.add { 
                        state.currentStep = Step.OPPONENT_TURN 
                        println(">> Player turn ended.")
                    }
                }
                else -> {}
            }

            choices.add("Surrender")
            actions.add {
                state.currentStep = Step.OPPONENT_TURN
                println(">> Game Over.")
            }

            println("\nAvailable Choices:")
            choices.forEachIndexed { i, desc -> println("${i + 1}: $desc") }
            
            print("\nSelect [1-${choices.size}] or 'q' to quit: ")
            val rawInput = readLine()?.trim() ?: "next"
            if (rawInput.lowercase() == "q") break

            val input = rawInput.toIntOrNull()
            if (input != null && input in 1..choices.size) {
                actions[input - 1].invoke()
                if (state.currentStep != Step.OPPONENT_TURN) {
                    if (state.currentStep in listOf(Step.MAIN, Step.ATTACK, Step.END)) {
                        state.printStatus()
                    } else {
                        state.autoProgress()
                    }
                }
            } else {
                println("Invalid input.")
            }
        }
    }

    private fun payCost(state: GameState, amount: Int) {
        var remaining = amount
        if (state.reserve >= remaining) {
            state.reserve -= remaining
            state.coreTrash += remaining
        } else {
            state.coreTrash += state.reserve
            remaining -= state.reserve
            state.reserve = 0
            state.hasSoulCoreInReserve = false
            state.coreTrash += 1 
        }
        println(">> Paid $amount cores.")
    }
}

fun main(args: Array<String>) = Simulate().main(args)

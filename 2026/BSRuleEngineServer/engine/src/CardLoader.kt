package bstools.model

import kotlinx.cinterop.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
object CardLoader {
    private val cardCache = mutableMapOf<String, CardData>()

    @OptIn(ExperimentalForeignApi::class)
    private fun readFileText(path: String): String? {
        val f = fopen(path, "rb") ?: return null
        fseek(f, 0, SEEK_END)
        val size = ftell(f)
        fseek(f, 0, SEEK_SET)
        if (size <= 0) {
            fclose(f)
            return ""
        }
        memScoped {
            val buf = allocArray<ByteVar>(size + 1)
            fread(buf, 1u, size.convert(), f)
            buf[size] = 0.toByte()
            fclose(f)
            return buf.toKString()
        }
    }

    fun loadCard(cardNo: String): CardData {
        if (cardCache.containsKey(cardNo)) {
            return cardCache[cardNo]!!
        }

        // 探索パス: ~/.bscards/yaml/$cardNo.yaml
        val userHome = getenv("USERPROFILE")?.toKString() ?: getenv("HOME")?.toKString() ?: "C:\\Users\\kyoya"
        val candidates = listOf(
            "$userHome\\.bscards\\yaml\\$cardNo.yaml",
            "$userHome/.bscards/yaml/$cardNo.yaml",
            ".bscards/yaml/$cardNo.yaml",
            "../.bscards/yaml/$cardNo.yaml"
        )

        var yamlText: String? = null
        for (cand in candidates) {
            val text = readFileText(cand)
            if (text != null && text.isNotEmpty()) {
                yamlText = text
                break
            }
        }

        if (yamlText != null) {
            var name = cardNo
            var cost = 3
            var symbols = "緑1"
            var reductionSymbols = "緑1"
            var categoryStr = "スピリット"
            val systems = mutableListOf<String>()
            val lvCosts = mutableListOf<Int>()
            val lvBps = mutableListOf<Int>()
            var imageUrl = "https://www.battlespirits.com/images/cardlist/$cardNo.webp"
            var effect = ""

            var inSideA = false
            var inSystems = false
            var inLvInfo = false

            for (rawLine in yamlText.lines()) {
                val line = rawLine.trim()
                if (line.startsWith("sideA:")) {
                    inSideA = true
                    continue
                }
                if (line.startsWith("sideB:")) {
                    inSideA = false
                    inSystems = false
                    inLvInfo = false
                    continue
                }

                if (inSideA) {
                    if (line.startsWith("name:")) {
                        name = line.substringAfter("name:").trim().trim('"', '\'')
                        inSystems = false
                        inLvInfo = false
                    } else if (line.startsWith("cost:")) {
                        cost = line.substringAfter("cost:").trim().toIntOrNull() ?: 3
                        inSystems = false
                        inLvInfo = false
                    } else if (line.startsWith("symbols:")) {
                        symbols = line.substringAfter("symbols:").trim().trim('"', '\'')
                        inSystems = false
                        inLvInfo = false
                    } else if (line.startsWith("reductionSymbols:")) {
                        reductionSymbols = line.substringAfter("reductionSymbols:").trim().trim('"', '\'')
                        inSystems = false
                        inLvInfo = false
                    } else if (line.startsWith("category:")) {
                        categoryStr = line.substringAfter("category:").trim().trim('"', '\'')
                        inSystems = false
                        inLvInfo = false
                    } else if (line.startsWith("imageUrl:")) {
                        imageUrl = line.substringAfter("imageUrl:").trim().trim('"', '\'')
                        inSystems = false
                        inLvInfo = false
                    } else if (line.startsWith("effect:")) {
                        effect = line.substringAfter("effect:").trim().trim('"', '\'')
                        inSystems = false
                        inLvInfo = false
                    } else if (line.startsWith("systems:")) {
                        inSystems = true
                        inLvInfo = false
                    } else if (line.startsWith("lvInfo:")) {
                        inLvInfo = true
                        inSystems = false
                    } else if (inSystems && line.startsWith("-")) {
                        systems.add(line.substringAfter("-").trim().trim('"', '\''))
                    } else if (inLvInfo && line.startsWith("-")) {
                        // "Lv,必要コア数,BP" 形式。BPを持たないネクサス等は3要素目が無い
                        val lvStr = line.substringAfter("-").trim().trim('"', '\'')
                        val parts = lvStr.split(",")
                        val reqCore = parts.getOrNull(1)?.trim()?.toIntOrNull()
                        if (reqCore != null) {
                            lvCosts.add(reqCore)
                            lvBps.add(parts.getOrNull(2)?.trim()?.toIntOrNull() ?: 0)
                        }
                    }
                }
            }

            val category = when {
                categoryStr.contains("ネクサス") -> CardCategory.NEXUS
                categoryStr.contains("マジック") -> CardCategory.MAGIC
                categoryStr.contains("ブレイヴ") -> CardCategory.BRAVE
                categoryStr.contains("アルティメット") -> CardCategory.ULTIMATE
                else -> CardCategory.SPIRIT
            }

            val card = CardData(
                cardNo = cardNo,
                name = name,
                cost = cost,
                colors = CardColor.fromString(symbols).ifEmpty { listOf(CardColor.GREEN) },
                reductionSymbols = CardColor.fromString(reductionSymbols),
                category = category,
                lvCosts = lvCosts.ifEmpty { if (category == CardCategory.SPIRIT) listOf(1) else listOf(0) },
                lvBps = lvBps,
                symbols = CardColor.fromString(symbols).ifEmpty { listOf(CardColor.GREEN) },
                systems = systems,
                effect = effect
            )
            cardCache[cardNo] = card
            return card
        }

        // カード個別マスタが見つからない場合は、公式カードリストから生成したデータ (OfficialCards.kt) を使う
        val official = OFFICIAL_CARDS[cardNo]
        if (official != null) {
            cardCache[cardNo] = official
            return official
        }

        println("⚠️ カード情報が見つかりません: $cardNo (仮の値で代用。engine/tools/fetch-official-cards.mjs で取得してください)")

        // 汎用フォールバック
        val fallback = CardData(
            cardNo = cardNo,
            name = cardNo,
            cost = 3,
            colors = listOf(CardColor.GREEN),
            reductionSymbols = listOf(CardColor.GREEN),
            category = CardCategory.SPIRIT,
            lvCosts = listOf(1),
            symbols = listOf(CardColor.GREEN),
            systems = listOf("甲魚")
        )
        cardCache[cardNo] = fallback
        return fallback
    }

    private val deckCache = mutableMapOf<String, List<CardData>>()

    fun loadDeck(deckPath: String): List<CardData> {
        deckCache[deckPath]?.let { return it }

        val candidates = listOf(
            deckPath,
            "decks/$deckPath",
            "../decks/$deckPath",
            "../BS/BSRust/decks/$deckPath",
            "../../BS/BSRust/decks/$deckPath"
        )

        var text: String? = null
        for (cand in candidates) {
            val t = readFileText(cand)
            if (t != null && t.isNotEmpty()) {
                text = t
                break
            }
        }

        if (text == null) {
            return emptyList()
        }

        val deckCards = mutableListOf<CardData>()
        var inCards = false

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.startsWith("cards:")) {
                inCards = true
                continue
            }
            if (line.startsWith("tokens:")) {
                inCards = false
                continue
            }

            if (inCards && line.contains(":")) {
                val commentName = if (line.contains("#")) line.substringAfter("#").trim() else ""
                val cleanLine = if (line.contains("#")) line.substringBefore("#").trim() else line
                val parts = cleanLine.split(":")
                if (parts.size >= 2) {
                    val cardNo = parts[0].trim().trim('"', '\'')
                    val count = parts[1].trim().toIntOrNull() ?: 1
                    if (cardNo.isNotEmpty()) {
                        var card = loadCard(cardNo)
                        if (commentName.isNotEmpty() && (card.name == cardNo || card.name.isEmpty())) {
                            card = card.copy(name = commentName)
                            cardCache[cardNo] = card
                        }
                        repeat(count) {
                            deckCards.add(card)
                        }
                    }
                }
            }
        }

        val immutableDeck = deckCards.toList()
        deckCache[deckPath] = immutableDeck
        return immutableDeck
    }
}

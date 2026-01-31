import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString
import com.charleskorn.kaml.*

class CypherGenerator {
    private val json = Json { ignoreUnknownKeys = true }

    fun yamlToJson(node: YamlNode): JsonElement = when (node) {
        is YamlScalar -> JsonPrimitive(node.content)
        is YamlList -> JsonArray(node.items.map { yamlToJson(it) })
        is YamlMap -> JsonObject(node.entries.map { it.key.content to yamlToJson(it.value) }.toMap())
        is YamlNull -> JsonNull
        else -> JsonNull
    }

    fun generateFromJson(cardJson: JsonObject): String {
        val cardNo = cardJson["cardNo"]?.jsonPrimitive?.content ?: return ""
        val safeId = sanitizeVar(cardNo)
        val sideA = cardJson["sideA"]?.jsonObject ?: return ""
        val sideB = cardJson["sideB"]?.let { if (it is JsonNull) null else it.jsonObject }
        
        val faces = listOfNotNull(sideA, sideB)
        val sb = StringBuilder()

        sb.append("MERGE (c_").append(safeId).append(":Card {cardNo: \"").append(sanitize(cardNo)).append("\"})\n")

        faces.forEach { face ->
            val side = face["side"]?.jsonPrimitive?.content ?: ""
            val name = face["name"]?.jsonPrimitive?.content ?: ""
            val rarity = face["rarity"]?.jsonPrimitive?.content ?: ""
            val cost = face["cost"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
            val symbols = face["symbols"]?.jsonPrimitive?.content ?: ""
            val reductionSymbols = face["reductionSymbols"]?.jsonPrimitive?.content ?: ""
            val category = face["category"]?.jsonPrimitive?.content ?: ""
            val attributes = splitAttributes(face["attributes"]?.jsonPrimitive?.content ?: "")
            val systems = face["systems"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val lvInfo = face["lvInfo"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            val effect = face["effect"]?.jsonPrimitive?.content ?: ""
            val restriction = face["restriction"]?.jsonPrimitive?.content ?: ""
            val imageUrl = face["imageUrl"]?.jsonPrimitive?.content ?: ""
            val keywords = extractKeywords(effect)
            val timings = extractTimings(effect)

            val faceVar = "cf_${safeId}_${side}"

            sb.append("MERGE (").append(faceVar).append(":CardFace {cardNo: \"").append(sanitize(cardNo)).append("\", side: \"").append(side).append("\"})\n")
            sb.append("SET ").append(faceVar).append(".name = \"").append(sanitize(name)).append("\", ")
              .append(faceVar).append(".cost = ").append(cost).append(", ")
              .append(faceVar).append(".symbols = \"").append(sanitize(symbols)).append("\", ")
              .append(faceVar).append(".reductionSymbols = \"").append(sanitize(reductionSymbols)).append("\", ")
              .append(faceVar).append(".lvInfo = ").append(Json.encodeToString(lvInfo)).append(", ")
              .append(faceVar).append(".effect = \"").append(sanitize(effect)).append("\", ")
              .append(faceVar).append(".restriction = \"").append(sanitize(restriction)).append("\", ")
              .append(faceVar).append(".imageUrl = \"").append(sanitize(imageUrl)).append("\"\n")

            sb.append("MERGE (c_").append(safeId).append(")-[:HAS_FACE {side: \"").append(side).append("\"}]->(").append(faceVar).append(")\n")

            // Category
            val catVar = "cat_${safeId}_${side}"
            sb.append("MERGE (").append(catVar).append(":Category {name: \"").append(sanitize(category)).append("\"})\n")
            sb.append("MERGE (").append(faceVar).append(")-[:IS_CATEGORY]->(").append(catVar).append(")\n")

            // Rarity
            val rarVar = "r_${safeId}_${side}"
            sb.append("MERGE (").append(rarVar).append(":Rarity {name: \"").append(sanitize(rarity)).append("\"})\n")
            sb.append("MERGE (").append(faceVar).append(")-[:HAS_RARITY]->(").append(rarVar).append(")\n")

            // Cost Node
            val costNodeVar = "cost_${safeId}_${side}"
            sb.append("MERGE (").append(costNodeVar).append(":Cost {value: ").append(cost).append("})\n")
            sb.append("MERGE (").append(faceVar).append(")-[:HAS_COST]->(").append(costNodeVar).append(")\n")

            // Colors
            attributes.forEachIndexed { i, col ->
                val colVar = "col_${safeId}_${side}_${i}"
                sb.append("MERGE (").append(colVar).append(":Color {name: \"").append(sanitize(col)).append("\"})\n")
                sb.append("MERGE (").append(faceVar).append(")-[:HAS_COLOR]->(").append(colVar).append(")\n")
            }

            // Systems
            systems.forEachIndexed { i, sys ->
                val sysVar = "sys_${safeId}_${side}_${i}"
                sb.append("MERGE (").append(sysVar).append(":System {name: \"").append(sanitize(sys)).append("\"})\n")
                sb.append("MERGE (").append(faceVar).append(")-[:HAS_SYSTEM]->(").append(sysVar).append(")\n")
            }

            // Keywords
            keywords.forEachIndexed { i, kw ->
                val kwVar = "k_${safeId}_${side}_${i}"
                sb.append("MERGE (").append(kwVar).append(":Keyword {name: \"").append(sanitize(kw)).append("\"})\n")
                sb.append("MERGE (").append(faceVar).append(")-[:HAS_KEYWORD]->(").append(kwVar).append(")\n")
            }

            // Timings
            timings.forEachIndexed { i, t ->
                val tVar = "t_${safeId}_${side}_${i}"
                sb.append("MERGE (").append(tVar).append(":Timing {name: \"").append(sanitize(t)).append("\"})\n")
                sb.append("MERGE (").append(faceVar).append(")-[:TRIGGERS_AT]->(").append(tVar).append(")\n")
            }
        }

        return sb.toString()
    }

    private fun sanitizeVar(input: String): String {
        return input.replace(Regex("[^a-zA-Z0-9]"), "_")
    }

    fun generate(card: Card): String {
        val faces = listOfNotNull(card.sideA, card.sideB)
        
        // Prepare data for UNWIND to avoid repetition in Cypher
        val facesData = faces.map { face ->
            mapOf(
                "side" to face.side,
                "name" to face.name,
                "rarity" to face.rarity,
                "cost" to face.cost,
                "symbols" to face.symbols,
                "reductionSymbols" to face.reductionSymbols,
                "category" to face.category,
                "attributes" to splitAttributes(face.attributes),
                "systems" to face.systems,
                "lvInfo" to face.lvInfo,
                "effect" to face.effect,
                "restriction" to face.restriction,
                "imageUrl" to face.imageUrl,
                "keywords" to extractKeywords(face.effect),
                "timings" to extractTimings(face.effect)
            )
        }

        val jsonFaces = Json.encodeToString(facesData)

        // Using safe interpolation for the cardNo, but passing complex data as a parameter is usually better.
        // However, for a generator tool producing a raw query string, embedding JSON for UNWIND is practical.
        return """
            MERGE (c:Card {cardNo: "${sanitize(card.cardNo)}"})
            WITH c
            UNWIND $jsonFaces AS faceData
            
            MERGE (cf:CardFace {cardNo: "${sanitize(card.cardNo)}", side: faceData.side})
            SET cf.name = faceData.name,
                cf.cost = faceData.cost,
                cf.symbols = faceData.symbols,
                cf.reductionSymbols = faceData.reductionSymbols,
                cf.lvInfo = faceData.lvInfo,
                cf.effect = faceData.effect,
                cf.restriction = faceData.restriction,
                cf.imageUrl = faceData.imageUrl
            
            MERGE (c)-[:HAS_FACE {side: faceData.side}]->(cf)
            
            // Category
            MERGE (cat:Category {name: faceData.category})
            MERGE (cf)-[:IS_CATEGORY]->(cat)
            
            // Rarity
            MERGE (r:Rarity {name: faceData.rarity})
            MERGE (cf)-[:HAS_RARITY]->(r)
            
            // Cost
            MERGE (costNode:Cost {value: faceData.cost})
            MERGE (cf)-[:HAS_COST]->(costNode)
            
            // Colors (Attributes)
            FOREACH (colorName IN faceData.attributes |
                MERGE (col:Color {name: colorName})
                MERGE (cf)-[:HAS_COLOR]->(col)
            )
            
            // Systems
            FOREACH (systemName IN faceData.systems |
                MERGE (sys:System {name: systemName})
                MERGE (cf)-[:HAS_SYSTEM]->(sys)
            )
            
            // Keywords (Extracted)
            FOREACH (keywordName IN faceData.keywords |
                MERGE (k:Keyword {name: keywordName})
                MERGE (cf)-[:HAS_KEYWORD]->(k)
            )
            
            // Timings (Extracted)
            FOREACH (timingName IN faceData.timings |
                MERGE (t:Timing {name: timingName})
                MERGE (cf)-[:TRIGGERS_AT]->(t)
            )
        """.trimIndent()
    }

    private fun sanitize(input: String): String {
        // Simple sanitization for Cypher strings (escape double quotes and backslashes)
        return input.replace("\\", "\\\\").replace("\"", "\\\"")
    }

    private fun splitAttributes(attributes: String): List<String> {
        // Assuming attributes are single characters like "赤紫" -> ["赤", "紫"]
        // If "全色" exists, it might need special handling, but currently splitting works for chars.
        // We filter out non-color-like characters if necessary, but data seems clean.
        return attributes.toList().map { it.toString() }
    }

    private fun extractKeywords(effect: String): List<String> {
        // Extract content inside 【...】
        val regex = Regex("【(.*?)】")
        return regex.findAll(effect)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }

    private fun extractTimings(effect: String): List<String> {
        // Extract content inside 『...』
        val regex = Regex("『(.*?)』")
        return regex.findAll(effect)
            .map { it.groupValues[1] }
            .distinct()
            .toList()
    }
}

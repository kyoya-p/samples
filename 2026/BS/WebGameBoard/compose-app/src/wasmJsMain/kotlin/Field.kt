import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch

enum class ZoneType { HAND, FIELD }

@Stable
class DragDropState {
    var draggedCard by mutableStateOf<CardInstance?>(null)
    var draggedStack by mutableStateOf<CardStack?>(null)
    var dragPosition by mutableStateOf(Offset.Zero)
    var grabOffset by mutableStateOf(Offset.Zero)
    var sourceZone by mutableStateOf<ZoneType?>(null)
    var hoverStackId by mutableStateOf<Int?>(null)
    var hoverCardId by mutableStateOf<Int?>(null)
    
    val zoneRects = mutableStateMapOf<ZoneType, Rect>()
    val stackRects = mutableStateMapOf<Int, Rect>()
    val cardRects = mutableStateMapOf<Int, Rect>()

    fun updateZoneRect(zone: ZoneType, rect: Rect) { zoneRects[zone] = rect }
    fun updateStackRect(stackId: Int, rect: Rect) { stackRects[stackId] = rect }
    fun updateCardRect(cardId: Int, rect: Rect) { cardRects[cardId] = rect }

    fun getTargetZone(position: Offset): ZoneType? {
        return zoneRects.entries.firstOrNull { it.value.contains(position) }?.key
    }
    
    fun getTargetStack(position: Offset, excludeStackId: Int? = null): Int? {
        return stackRects.entries
            .filter { it.key != excludeStackId }
            .firstOrNull { it.value.contains(position) }?.key
    }
    
    fun getTargetCard(position: Offset, excludeCardId: Int? = null): Int? {
        return cardRects.entries
            .filter { it.key != excludeCardId }
            .firstOrNull { it.value.contains(position) }?.key
    }
}

@Composable
fun App() {
    val coroutineScope = rememberCoroutineScope()
    val dragDropState = remember { DragDropState() }
    
    val handCards = remember { mutableStateListOf<CardInstance>() }
    val fieldCards = remember { mutableStateListOf<CardInstance>() }
    val fieldStacks = remember { mutableStateListOf<CardStack>() }

    var idCounter by remember { mutableStateOf(0) }
    var deckYaml by remember { mutableStateOf("") }

    fun cleanStacks() {
        // Convert single-card stacks back to fieldCards
        val singles = fieldStacks.filter { it.cards.size == 1 }
        singles.forEach { stack ->
            val card = stack.cards.removeAt(0)
            card.offset = stack.offset
            fieldCards.add(card)
            fieldStacks.remove(stack)
        }
        // Remove empty
        fieldStacks.removeAll { it.cards.isEmpty() }
        
        // Cleanup rects
        val activeStackIds = fieldStacks.map { it.id }.toSet()
        dragDropState.stackRects.keys.retainAll(activeStackIds)
        val activeCardIds = fieldCards.map { it.id }.toSet()
        dragDropState.cardRects.keys.retainAll(activeCardIds)
    }

    fun moveCard(instance: CardInstance, from: ZoneType, to: ZoneType, dropPos: Offset, grab: Offset, targetStackId: Int?, targetCardId: Int?) {
        handCards.remove(instance)
        fieldCards.remove(instance)
        fieldStacks.forEach { it.cards.remove(instance) }

        val fieldRect = dragDropState.zoneRects[ZoneType.FIELD] ?: Rect.Zero
        val dropOffset = dropPos - fieldRect.topLeft - grab

        when (to) {
            ZoneType.HAND -> {
                instance.isFlipped = false
                handCards.add(instance)
            }
            ZoneType.FIELD -> {
                val targetStack = fieldStacks.find { it.id == targetStackId }
                val targetCard = fieldCards.find { it.id == targetCardId }

                if (targetStack != null) {
                    instance.rotation = targetStack.cards.lastOrNull()?.rotation ?: 0f
                    targetStack.cards.add(instance)
                } else if (targetCard != null) {
                    val newStack = CardStack(idCounter++, targetCard.offset)
                    instance.rotation = targetCard.rotation
                    newStack.cards.add(targetCard)
                    newStack.cards.add(instance)
                    fieldCards.remove(targetCard)
                    fieldStacks.add(newStack)
                } else {
                    instance.offset = dropOffset
                    fieldCards.add(instance)
                }
            }
        }
        cleanStacks()
    }

    fun moveStack(stack: CardStack, dropPos: Offset, grab: Offset, targetStackId: Int?, targetCardId: Int?) {
        val targetStack = fieldStacks.find { it.id == targetStackId }
        val targetCard = fieldCards.find { it.id == targetCardId }

        if (targetStack != null && targetStack != stack) {
            val topRot = targetStack.cards.lastOrNull()?.rotation ?: 0f
            stack.cards.forEach { it.rotation = topRot }
            targetStack.cards.addAll(stack.cards)
            fieldStacks.remove(stack)
        } else if (targetCard != null) {
            val newStack = CardStack(idCounter++, targetCard.offset)
            stack.cards.forEach { it.rotation = targetCard.rotation }
            newStack.cards.add(targetCard)
            newStack.cards.addAll(stack.cards)
            fieldCards.remove(targetCard)
            fieldStacks.remove(stack)
            fieldStacks.add(newStack)
        } else {
            val fieldRect = dragDropState.zoneRects[ZoneType.FIELD] ?: Rect.Zero
            stack.offset = dropPos - fieldRect.topLeft - grab
        }
        cleanStacks()
    }

    fun resetGame() {
        val all = (handCards + fieldCards + fieldStacks.flatMap { it.cards }).toList()
        handCards.clear(); fieldCards.clear(); fieldStacks.clear()
        if (all.isNotEmpty()) {
            val deckStack = CardStack(idCounter++, Offset(1200f, 50f))
            all.forEach { it.isFlipped = true; it.rotation = 0f }
            deckStack.cards.addAll(all)
            fieldStacks.add(deckStack)
        }
    }

    fun startDeck(yaml: String) {
        handCards.clear(); fieldCards.clear(); fieldStacks.clear()
        val lines = yaml.ifBlank { "bs01-001:1" }.split("\n")
        var ox = 50f; var oy = 50f
        lines.forEach { line ->
            val parts = line.split(":")
            if (parts.size < 2) return@forEach
            val id = parts[0].trim().uppercase()
            val count = parts[1].trim().toIntOrNull() ?: 1
            val card = SearchCard(id, "Card $id", "", "", "", "", emptyList(), "https://www.battlespirits.com/images/cardlist/$id.webp")
            repeat(count) {
                val inst = CardInstance(idCounter++, card)
                inst.offset = Offset(ox, oy)
                fieldCards.add(inst)
                ox += 120f
                if (ox > 1000f) { ox = 50f; oy += 150f }
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Main Layout
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header / Control
                    Row(modifier = Modifier.fillMaxWidth().height(80.dp).background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        TextField(value = deckYaml, onValueChange = { deckYaml = it }, placeholder = { Text("bs01-001:40") }, modifier = Modifier.width(250.dp))
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = { startDeck(deckYaml) }) { Text("Start") }
                        Spacer(Modifier.weight(1f))
                        Button(onClick = { resetGame() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Reset") }
                    }

                    // Field
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().onGloballyPositioned { dragDropState.updateZoneRect(ZoneType.FIELD, it.boundsInWindow()) }) {
                        Text("FIELD", modifier = Modifier.align(Alignment.Center), color = Color.White.copy(alpha = 0.1f), fontSize = 100.sp)
                        
                        // Render Cards & Stacks
                        fieldCards.forEach { card ->
                            Box(modifier = Modifier.offset { IntOffset(card.offset.x.toInt(), card.offset.y.toInt()) }) {
                                DraggableCard(instance = card, zone = ZoneType.FIELD, dragDropState = dragDropState, onMove = ::moveCard)
                            }
                        }
                        fieldStacks.forEach { stack ->
                            CardStackView(stack = stack, dragDropState = dragDropState, onMoveCard = ::moveCard, onMoveStack = ::moveStack)
                        }
                    }

                    // Hand
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp).background(MaterialTheme.colorScheme.secondaryContainer)
                        .onGloballyPositioned { dragDropState.updateZoneRect(ZoneType.HAND, it.boundsInWindow()) }) {
                        Row(modifier = Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            handCards.forEach { card ->
                                DraggableCard(instance = card, zone = ZoneType.HAND, dragDropState = dragDropState, onMove = ::moveCard, isPrivate = true)
                            }
                        }
                    }
                }

                // Drag Overlay
                dragDropState.draggedCard?.let { instance ->
                    val pos = dragDropState.dragPosition - dragDropState.grabOffset
                    Box(modifier = Modifier.offset { IntOffset(pos.x.toInt(), pos.y.toInt()) }.size(75.dp, 105.dp).zIndex(1000f)) {
                        CardPlaceholder(name = instance.card.name, isFlipped = instance.isFlipped, rotation = instance.rotation)
                    }
                }
                dragDropState.draggedStack?.let { stack ->
                    val pos = dragDropState.dragPosition - dragDropState.grabOffset
                    Box(modifier = Modifier.offset { IntOffset(pos.x.toInt(), pos.y.toInt()) }.zIndex(1000f)) {
                        // Simplified stack preview
                        CardPlaceholder(name = "Stack", isFlipped = true)
                    }
                }
            }
        }
    }
}

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

enum class ZoneType { HAND, FIELD, TRASH, DECK, BURST, LIFE, RESERVE }

@Stable
class DragDropState {
    var draggedCard by mutableStateOf<CardInstance?>(null)
    var draggedStack by mutableStateOf<CardStack?>(null)
    var dragPosition by mutableStateOf(Offset.Zero)
    var grabOffset by mutableStateOf(Offset.Zero)
    var sourceZone by mutableStateOf<ZoneType?>(null)
    var hoverStackId by mutableStateOf<Int?>(null)
    
    val zoneRects = mutableStateMapOf<ZoneType, Rect>()
    val stackRects = mutableStateMapOf<Int, Rect>()

    fun updateZoneRect(zone: ZoneType, rect: Rect) {
        zoneRects[zone] = rect
    }
    
    fun updateStackRect(stackId: Int, rect: Rect) {
        stackRects[stackId] = rect
    }

    fun getTargetZone(position: Offset): ZoneType? {
        return zoneRects.entries.firstOrNull { it.value.contains(position) }?.key
    }
    
    fun getTargetStack(position: Offset, excludeStackId: Int? = null): Int? {
        return stackRects.entries
            .filter { it.key != excludeStackId }
            .firstOrNull { it.value.contains(position) }?.key
    }
}

@Composable
fun App() {
    var lifeCores by remember { mutableStateOf(5) }
    var reserveCores by remember { mutableStateOf(4) }
    var soulCoreInReserve by remember { mutableStateOf(true) }
    var trashCores by remember { mutableStateOf(0) }

    val handCards = remember { mutableStateListOf<CardInstance>() }
    val fieldStacks = remember { mutableStateListOf<CardStack>() }

    var instanceIdCounter by remember { mutableStateOf(0) }
    var stackIdCounter by remember { mutableStateOf(0) }
    val dragDropState = remember { DragDropState() }

    fun moveCard(instance: CardInstance, from: ZoneType, to: ZoneType, dropPos: Offset, grab: Offset, targetStackId: Int? = null) {
        handCards.remove(instance)
        fieldStacks.forEach { it.cards.remove(instance) }
        fieldStacks.removeAll { it.cards.isEmpty() }
        val activeIds = fieldStacks.map { it.id }.toSet()
        dragDropState.stackRects.keys.retainAll(activeIds)

        when (to) {
            ZoneType.HAND -> {
                handCards.add(instance)
            }
            ZoneType.FIELD -> {
                val targetStack = fieldStacks.find { it.id == targetStackId }
                if (targetStack != null) {
                    targetStack.cards.lastOrNull()?.let { instance.rotation = it.rotation }
                    targetStack.cards.add(instance)
                } else {
                    val fieldRect = dragDropState.zoneRects[ZoneType.FIELD] ?: Rect.Zero
                    val newStack = CardStack(stackIdCounter++, dropPos - fieldRect.topLeft - grab)
                    newStack.cards.add(instance)
                    fieldStacks.add(newStack)
                }
            }
            else -> {}
        }
    }
    
    fun moveStack(stack: CardStack, dropPos: Offset, grab: Offset, targetStackId: Int? = null) {
        val targetStack = fieldStacks.find { it.id == targetStackId }
        if (targetStack != null && targetStack != stack) {
            targetStack.cards.lastOrNull()?.let { top ->
                stack.cards.forEach { it.rotation = top.rotation }
            }
            targetStack.cards.addAll(stack.cards)
            fieldStacks.remove(stack)
            dragDropState.stackRects.remove(stack.id)
        } else {
            val fieldRect = dragDropState.zoneRects[ZoneType.FIELD] ?: Rect.Zero
            stack.offset = dropPos - fieldRect.topLeft - grab
            fieldStacks.remove(stack)
            fieldStacks.add(stack)
        }
    }

    fun initializeGame() {
        lifeCores = 5; reserveCores = 4; soulCoreInReserve = true; trashCores = 0
        instanceIdCounter = 0; stackIdCounter = 0
        fieldStacks.clear(); handCards.clear()
        
        val phoenix = SearchCard("BS68-X01", "Phoenix Golem", "X", "8", "S", "Blue", listOf("Artificer", "Phoenix"), "")
        val seaKing = SearchCard("BS68-X02", "Sea King", "X", "7", "S", "Blue", listOf("Fighter"), "")
        
        val allCards = (0 until 20).flatMap { 
            listOf(CardInstance(instanceIdCounter++, phoenix), CardInstance(instanceIdCounter++, seaKing))
        }.onEach { 
            it.isFlipped = true 
            it.rotation = 0f
        }
        
        // Place initial deck at Top-Right of field
        val deckStack = CardStack(stackIdCounter++, Offset(650f, 20f))
        deckStack.cards.addAll(allCards)
        fieldStacks.add(deckStack)
    }

    LaunchedEffect(Unit) { initializeGame() }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(0.5f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant), 
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Opponent Area (Life: 5)", modifier = Modifier.padding(start = 16.dp))
                        Button(onClick = { initializeGame() }, modifier = Modifier.padding(end = 16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Reset")
                        }
                    }

                    Row(modifier = Modifier.weight(3f).fillMaxWidth()) {
                        // Left Column
                        Column(modifier = Modifier.width(120.dp).fillMaxHeight().padding(8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            ZoneBox("Burst", modifier = Modifier.weight(1f).padding(bottom = 8.dp))
                            ZoneBox("Life: $lifeCores", modifier = Modifier.weight(1.5f).padding(bottom = 8.dp), color = Color(0xFF6200EE))
                            ZoneBox("Reserve: $reserveCores" + if (soulCoreInReserve) "\n(+S)" else "", 
                                modifier = Modifier.weight(2f), color = Color(0xFF2C2C2C))
                        }

                        // Wide Field
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().padding(8.dp)
                            .onGloballyPositioned { dragDropState.updateZoneRect(ZoneType.FIELD, it.boundsInWindow()) }
                            .background(Color(0xFF1E1E1E), shape = MaterialTheme.shapes.medium)) {
                            Text("Field", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(8.dp))
                            
                            fieldStacks.forEach { stack ->
                                CardStackView(stack, dragDropState, onMoveCard = ::moveCard, onMoveStack = ::moveStack)
                            }
                        }
                    }

                    // Hand (Private Area)
                    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)
                        .onGloballyPositioned { dragDropState.updateZoneRect(ZoneType.HAND, it.boundsInWindow()) }) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Hand (${handCards.size})", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { 
                                    // Draw from field stacks
                                    val deck = fieldStacks.find { it.cards.any { c -> c.isFlipped } }
                                    if (deck != null && deck.cards.isNotEmpty()) {
                                        handCards.add(deck.cards.removeAt(deck.cards.size - 1))
                                        if (deck.cards.isEmpty()) fieldStacks.remove(deck)
                                    }
                                }) { Text("Draw") }
                                Button(onClick = { soulCoreInReserve = !soulCoreInReserve }) { Text("S-Core") }
                            }
                        }
                        Row(modifier = Modifier.height(120.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            handCards.forEach { instance ->
                                DraggableCard(
                                    instance = instance, 
                                    zone = ZoneType.HAND, 
                                    dragDropState = dragDropState, 
                                    onMove = { i, f, t, p, g, ts -> moveCard(i, f, t, p, g, ts) },
                                    isPrivate = true
                                )
                            }
                        }
                    }
                }

                // Drag Overlay
                dragDropState.draggedCard?.let { instance ->
                    val overlayPos = dragDropState.dragPosition - dragDropState.grabOffset
                    Box(modifier = Modifier.offset { IntOffset(overlayPos.x.toInt(), overlayPos.y.toInt()) }) {
                        CardPlaceholder(instance.card.name, isFlipped = instance.isFlipped, isPrivate = true, rotation = instance.rotation, elevation = 12.dp)
                    }
                }
                dragDropState.draggedStack?.let { stack ->
                    val overlayPos = dragDropState.dragPosition - dragDropState.grabOffset
                    Box(modifier = Modifier.offset { IntOffset(overlayPos.x.toInt(), overlayPos.y.toInt()) }) {
                        CardStackContent(stack, elevation = 8.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun ZoneBox(label: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.secondaryContainer) {
    Surface(modifier = modifier.fillMaxWidth(), color = color, shape = MaterialTheme.shapes.small,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

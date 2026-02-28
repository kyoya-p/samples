import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.zIndex
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.key.*
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
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

    fun updateZoneRect(zone: ZoneType, rect: Rect) {
        zoneRects[zone] = rect
    }
    
    fun updateStackRect(stackId: Int, rect: Rect) {
        stackRects[stackId] = rect
    }
    
    fun updateCardRect(cardId: Int, rect: Rect) {
        cardRects[cardId] = rect
    }

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

// Global JS bridge for logging is now in Main.kt

@Composable
fun App() {
    log("App Recomposed")

    val handCards = remember { mutableStateListOf<CardInstance>() }
    val fieldCards = remember { mutableStateListOf<CardInstance>() }
    val fieldStacks = remember { mutableStateListOf<CardStack>() }

    var instanceIdCounter by remember { mutableStateOf(0) }
    var stackIdCounter by remember { mutableStateOf(0) }
    val dragDropState = remember { DragDropState() }
    val focusRequester = remember { FocusRequester() }
    var deckYaml by remember { mutableStateOf("") }

    fun cleanEmptyStacks() {
        log("cleanEmptyStacks called. fieldStacks size before: " + fieldStacks.size)
        fieldStacks.removeAll { it.cards.isEmpty() }
        // Convert 1-card stacks to fieldCards
        val singleStacks = fieldStacks.filter { it.cards.size == 1 }
        log("Found singleStacks count: " + singleStacks.size)
        singleStacks.forEach { stack ->
            val card = stack.cards.removeAt(0)
            card.offset = stack.offset
            fieldCards.add(card)
            val removed = fieldStacks.remove(stack)
            log("Removed single stack ID " + stack.id + ", success: " + removed + ", new fieldCards size: " + fieldCards.size)
        }
        
        val activeStackIds = fieldStacks.map { it.id }.toSet()
        dragDropState.stackRects.keys.retainAll(activeStackIds)
        val activeCardIds = fieldCards.map { it.id }.toSet()
        dragDropState.cardRects.keys.retainAll(activeCardIds)
    }

    fun moveCard(instance: CardInstance, from: ZoneType, to: ZoneType, dropPos: Offset, grab: Offset, targetStackId: Int? = null, targetCardId: Int? = null) {
        log("moveCard called. Instance ID: " + instance.id + ", to: " + to + ", dropPos: " + dropPos + ", grab: " + grab)
        handCards.remove(instance)
        fieldCards.remove(instance)
        fieldStacks.forEach { it.cards.remove(instance) }

        when (to) {
            ZoneType.HAND -> {
                handCards.add(instance)
            }
            ZoneType.FIELD -> {
                val fieldRect = dragDropState.zoneRects[ZoneType.FIELD] ?: Rect.Zero
                val dropOffset = dropPos - fieldRect.topLeft - grab
                log("FIELD move calculation: fieldRect.topLeft=" + fieldRect.topLeft + ", result dropOffset=" + dropOffset)
                
                val targetStack = fieldStacks.find { it.id == targetStackId }
                val targetCard = fieldCards.find { it.id == targetCardId }
                
                if (targetStack != null) {
                    log("Targeting stack ID: " + targetStackId)
                    targetStack.cards.lastOrNull()?.let { instance.rotation = it.rotation }
                    targetStack.cards.add(instance)
                } else if (targetCard != null) {
                    log("Targeting card ID: " + targetCardId + " to create new stack")
                    val newStack = CardStack(stackIdCounter++, targetCard.offset)
                    instance.rotation = targetCard.rotation
                    newStack.cards.add(targetCard)
                    newStack.cards.add(instance)
                    fieldCards.remove(targetCard)
                    fieldStacks.add(newStack)
                } else {
                    log("Placing as single card at " + dropOffset)
                    instance.offset = dropOffset
                    fieldCards.add(instance)
                }
            }
            else -> {}
        }
        cleanEmptyStacks()
    }
    
    fun moveStack(stack: CardStack, dropPos: Offset, grab: Offset, targetStackId: Int? = null, targetCardId: Int? = null) {
        val targetStack = fieldStacks.find { it.id == targetStackId }
        val targetCard = fieldCards.find { it.id == targetCardId }
        
        if (targetStack != null && targetStack != stack) {
            targetStack.cards.lastOrNull()?.let { top ->
                stack.cards.forEach { it.rotation = top.rotation }
            }
            targetStack.cards.addAll(stack.cards)
            fieldStacks.remove(stack)
        } else if (targetCard != null) {
            val newStack = CardStack(stackIdCounter++, targetCard.offset)
            stack.cards.forEach { it.rotation = targetCard.rotation }
            newStack.cards.add(targetCard)
            newStack.cards.addAll(stack.cards)
            fieldCards.remove(targetCard)
            fieldStacks.remove(stack)
            fieldStacks.add(newStack)
        } else {
            val fieldRect = dragDropState.zoneRects[ZoneType.FIELD] ?: Rect.Zero
            stack.offset = dropPos - fieldRect.topLeft - grab
            fieldStacks.remove(stack)
            fieldStacks.add(stack)
        }
        cleanEmptyStacks()
    }

    fun initializeGame() {
        log("initializeGame called")
        instanceIdCounter = 0; stackIdCounter = 0
        fieldStacks.clear(); fieldCards.clear(); handCards.clear()
    }

        suspend fun startDeck(yaml: String) {
            initializeGame()
            val actualYaml = if (yaml.isBlank()) "bs01-001:1" else yaml
            log("Starting deck from YAML: " + actualYaml)
            val lines = actualYaml.split("\n")
            
            var currentOffsetX = 50f
            val currentOffsetY = 20f
            var totalCards = 0
    
            lines.forEach { line ->
                if (line.isBlank() || !line.contains(":")) return@forEach
                val parts = line.split(":")
                val id = parts[0].trim().uppercase()
                val count = parts[1].trim().toIntOrNull() ?: 1
                
                // Generic name generation or hardcoded fallback
                val name = when (id) {
                    "BS01-001" -> "ゴラドン"
                    "BS01-002" -> "ドラグノ祈祷師"
                    "BS68-X01" -> "鳳凰竜フェニックス・ゴレム"
                    else -> "Card $id"
                }
                
                val imgUrl = "https://www.battlespirits.com/images/cardlist/${id}.webp"
                val card = SearchCard(id, name, "C", "0", "S", "Red", emptyList(), imgUrl)
    
                repeat(count) {
                    val inst = CardInstance(instanceIdCounter++, card)
                    inst.isFlipped = false 
                    inst.rotation = 0f
                    val deckStack = CardStack(stackIdCounter++, Offset(currentOffsetX, currentOffsetY))
                    deckStack.cards.add(inst)
                    fieldStacks.add(deckStack)
                    currentOffsetX += 120f
                    if (currentOffsetX > 800f) {
                        currentOffsetX = 50f
                        // Move down if row is full? For now just keep it simple
                    }
                    totalCards++
                }
            }
            if (totalCards > 0) {
                log("Deck started with " + totalCards + " cards")
            }
            cleanEmptyStacks()
        }
    LaunchedEffect(Unit) { 
        initializeGame()
        focusRequester.requestFocus()
    }

    val drawCard = {
        val deck = fieldStacks.find { it.cards.any { c -> c.isFlipped } }
        if (deck != null && deck.cards.isNotEmpty()) {
            val card = deck.cards.removeAt(deck.cards.size - 1)
            card.isFlipped = false 
            handCards.add(card)
            log("Card drawn. Hand size: " + handCards.size)
            if (deck.cards.isEmpty()) fieldStacks.remove(deck)
        }
    }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { 
                if (it.type == KeyEventType.KeyDown && it.key == Key.D) {
                    drawCard()
                    true
                } else false
            }
        ) {
            Box(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures { offset ->
                    log("BOX CLICK at $offset")
                }
            }) {
                val coroutineScope = rememberCoroutineScope()
                val onMoveCard: (CardInstance, ZoneType, ZoneType, Offset, Offset, Int?, Int?) -> Unit = { i, f, t, p, g, ts, tc -> moveCard(i, f, t, p, g, ts, tc) }
                val onMoveStack: (CardStack, Offset, Offset, Int?, Int?) -> Unit = { s, p, g, ts, tc -> moveStack(s, p, g, ts, tc) }

                Column(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.weight(0.5f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant), 
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Opponent Area (Life: 5)", modifier = Modifier.padding(start = 16.dp))
                            Spacer(Modifier.width(20.dp))
                            TextField(
                                value = deckYaml,
                                onValueChange = { deckYaml = it },
                                placeholder = { Text("bs01-001:1") },
                                modifier = Modifier.width(200.dp).height(50.dp).onGloballyPositioned {
                                    val b = it.boundsInWindow()
                                    log("Deck YAML TextField Bounds: L=" + b.left + ", T=" + b.top + ", R=" + b.right + ", B=" + b.bottom)
                                }
                            )
                            Button(onClick = { 
                                val yaml = deckYaml
                                log("Start Button Clicked with yaml: " + yaml)
                                coroutineScope.launch { startDeck(yaml) } 
                            }, modifier = Modifier.onGloballyPositioned {
                                val b = it.boundsInWindow()
                                log("Start Button Bounds: L=" + b.left + ", T=" + b.top + ", R=" + b.right + ", B=" + b.bottom)
                            }) { Text("Start") }
                        }
                        Button(
                            onClick = { initializeGame() }, 
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .onGloballyPositioned {
                                    val b = it.boundsInWindow()
                                    log("Reset Button Bounds: L=" + b.left + ", T=" + b.top + ", R=" + b.right + ", B=" + b.bottom)
                                },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                            Text("Reset")
                        }
                    }

                    Row(modifier = Modifier.weight(3f).fillMaxWidth()) {
                        Box(modifier = Modifier.fillMaxSize().padding(8.dp)
                            .onGloballyPositioned { dragDropState.updateZoneRect(ZoneType.FIELD, it.boundsInWindow()) }) {
                            Text("Field", style = MaterialTheme.typography.labelLarge, color = Color.Gray, modifier = Modifier.padding(8.dp))
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp)
                        .onGloballyPositioned { dragDropState.updateZoneRect(ZoneType.HAND, it.boundsInWindow()) }) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Hand (${handCards.size})", style = MaterialTheme.typography.labelMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { 
                                        log("Draw button clicked!")
                                        drawCard()
                                    },
                                    modifier = Modifier
                                        .height(50.dp)
                                        .padding(horizontal = 4.dp)
                                        .onGloballyPositioned {
                                            val b = it.boundsInWindow()
                                            log("Draw Button Bounds: L=" + b.left + ", T=" + b.top + ", R=" + b.right + ", B=" + b.bottom)
                                        }
                                ) { Text("Draw", fontSize = 18.sp) }
                            }
                        }
                        Row(modifier = Modifier.height(130.dp).fillMaxWidth().padding(horizontal = 8.dp), 
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            handCards.forEach { instance ->
                                DraggableCard(
                                    instance = instance, 
                                    zone = ZoneType.HAND, 
                                    dragDropState = dragDropState, 
                                    onMove = onMoveCard,
                                    onDrawCard = null,
                                    isPrivate = true,
                                    modifier = Modifier.size(110.dp)
                                )
                            }
                        }
                    }
                }

                // Drag Overlay
                dragDropState.draggedCard?.let { instance ->
                    val overlayPos = dragDropState.dragPosition - dragDropState.grabOffset
                    Box(modifier = Modifier.offset { IntOffset(overlayPos.x.toInt(), overlayPos.y.toInt()) }.zIndex(100f)) {
                        CardPlaceholder(instance.card.name, imgUrl = instance.card.imgUrl, isFlipped = instance.isFlipped, isPrivate = true, rotation = instance.rotation, elevation = 12.dp)
                    }
                }
                dragDropState.draggedStack?.let { stack ->
                    val overlayPos = dragDropState.dragPosition - dragDropState.grabOffset
                    Box(modifier = Modifier.offset { IntOffset(overlayPos.x.toInt(), overlayPos.y.toInt()) }.zIndex(100f)) {
                        CardStackContent(stack, dragDropState, onMoveCard, onMoveStack, elevation = 8.dp)
                    }
                }

                // Render Field Cards in a non-blocking overlay (MUST BE LAST TO BE ON TOP)
                Box(modifier = Modifier.fillMaxSize()) {
                    fieldCards.forEach { card ->
                        Box(modifier = Modifier.offset { IntOffset(card.offset.x.toInt(), card.offset.y.toInt()) }) {
                            DraggableCard(
                                instance = card,
                                zone = ZoneType.FIELD,
                                dragDropState = dragDropState,
                                onMove = onMoveCard,
                                onDrawCard = { i ->
                                    fieldCards.remove(i)
                                    i.isFlipped = false
                                    handCards.add(i)
                                    cleanEmptyStacks()
                                    log("Card drawn from field. Hand size: " + handCards.size)
                                }
                            )
                        }
                    }
                    
                    fieldStacks.forEach { stack ->
                        CardStackView(
                            stack = stack, 
                            dragDropState = dragDropState, 
                            onMoveCard = onMoveCard, 
                            onMoveStack = onMoveStack,
                            onDrawCard = { card ->
                                stack.cards.remove(card)
                                card.isFlipped = false
                                handCards.add(card)
                                cleanEmptyStacks()
                                log("Card drawn from stack. Hand size: " + handCards.size)
                            }
                        )
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

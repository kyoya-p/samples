import androidx.compose.foundation.background
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.ktor.client.request.get
import io.ktor.client.call.body
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

val globalHttpClient = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}

private val imageCache = mutableMapOf<String, ImageBitmap>()

class CardInstance(val id: Int, val card: SearchCard) {
// ... (rest of the code)
    var isFlipped by mutableStateOf(false)
    var rotation by mutableStateOf(0f)
    var offset by mutableStateOf(Offset.Zero)
}

class CardStack(val id: Int, initialOffset: Offset = Offset.Zero) {
    val cards = mutableStateListOf<CardInstance>()
    var offset by mutableStateOf(initialOffset)
    var isSpread by mutableStateOf(false)
}

@Composable
fun CardStackView(
    stack: CardStack,
    dragDropState: DragDropState,
    onMoveCard: (CardInstance, ZoneType, ZoneType, Offset, Offset, Int?, Int?) -> Unit,
    onMoveStack: (CardStack, Offset, Offset, Int?, Int?) -> Unit,
    onDrawCard: (CardInstance) -> Unit
) {
    var globalOffset by remember { mutableStateOf(Offset.Zero) }
    val isHovered = dragDropState.hoverStackId == stack.id
    val isDraggingStack = dragDropState.draggedStack?.id == stack.id

    val spreadX = 38.dp
    val stackExtentX: Dp
    val stackExtentY: Dp
    if (stack.isSpread) {
        stackExtentX = 75.dp + (spreadX * (stack.cards.size - 1))
        stackExtentY = 105.dp
    } else {
        val extra = if (stack.cards.size > 1) (10 + (stack.cards.size - 2) * 2).dp else 0.dp
        stackExtentX = 75.dp + extra
        stackExtentY = 105.dp + extra
    }
    val squareSize = maxOf(stackExtentX, stackExtentY, 105.dp) + 30.dp

    Box(modifier = Modifier
        .offset { IntOffset(stack.offset.x.toInt(), stack.offset.y.toInt()) }
        .size(squareSize)
        .onGloballyPositioned { 
            globalOffset = it.positionInWindow()
            val b = it.boundsInWindow()
            log("Stack Handler Bounds: L=" + b.left + ", T=" + b.top + ", R=" + b.right + ", B=" + b.bottom)
            dragDropState.updateStackRect(stack.id, it.boundsInWindow()) 
        }
        .alpha(if (isDraggingStack) 0f else 1f)
        .border(if (isHovered) 4.dp else 0.dp, Color.Yellow, MaterialTheme.shapes.small)
    ) {
        // Background layer for Stack Drag (only triggers on "margin")
        Box(modifier = Modifier.fillMaxSize().pointerInput(stack.id) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                var dragStarted = false
                
                drag(down.id) { change ->
                    if (!dragStarted) {
                        dragStarted = true
                        dragDropState.draggedStack = stack
                        dragDropState.sourceZone = ZoneType.FIELD
                        dragDropState.grabOffset = down.position
                        dragDropState.dragPosition = globalOffset + down.position
                    }
                    change.consume()
                    dragDropState.dragPosition += change.positionChange()
                    dragDropState.hoverStackId = dragDropState.getTargetStack(dragDropState.dragPosition, stack.id)
                    dragDropState.hoverCardId = dragDropState.getTargetCard(dragDropState.dragPosition)
                }
                
                if (dragDropState.draggedStack != null) {
                    val targetStackId = dragDropState.getTargetStack(dragDropState.dragPosition, stack.id)
                    val targetCardId = dragDropState.getTargetCard(dragDropState.dragPosition)
                    onMoveStack(stack, dragDropState.dragPosition, dragDropState.grabOffset, targetStackId, targetCardId)
                    dragDropState.draggedStack = null
                    dragDropState.hoverStackId = null
                    dragDropState.hoverCardId = null
                }
            }
        })

        // Content Area
        CardStackContent(stack, dragDropState, onMoveCard, onMoveStack)
        
        // Icons Area (Above content)
        if (stack.cards.size > 1) {
            Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                HandlerIcon("F", Color(0xFF4CAF50), Modifier.align(Alignment.TopStart)) {
                    stack.cards.lastOrNull()?.let { it.isFlipped = !it.isFlipped }
                }
                HandlerIcon("R", Color(0xFFF44336), Modifier.align(Alignment.TopEnd)) {
                    val newRot = ((stack.cards.lastOrNull()?.rotation ?: 0f) + 90f) % 360f
                    stack.cards.forEach { it.rotation = newRot }
                }
                HandlerIcon("S", Color(0xFF9C27B0), Modifier.align(Alignment.BottomCenter),
                    onDragStart = { absPos ->
                        dragDropState.draggedStack = stack
                        dragDropState.grabOffset = absPos - globalOffset
                        dragDropState.dragPosition = absPos
                    },
                    onDrag = { dragDropState.dragPosition += it },
                    onDragEnd = {
                        val tStack = dragDropState.getTargetStack(dragDropState.dragPosition, stack.id)
                        val tCard = dragDropState.getTargetCard(dragDropState.dragPosition)
                        onMoveStack(stack, dragDropState.dragPosition, dragDropState.grabOffset, tStack, tCard)
                        dragDropState.draggedStack = null
                    }
                )
                HandlerIcon("E${stack.cards.size}", Color(0xFFE91E63), Modifier.align(Alignment.BottomEnd).size(30.dp)) {
                    stack.isSpread = !stack.isSpread
                }
            }
        }
    }
}

@Composable
fun CardStackContent(
    stack: CardStack,
    dragDropState: DragDropState? = null,
    onMoveCard: ((CardInstance, ZoneType, ZoneType, Offset, Offset, Int?, Int?) -> Unit)? = null,
    onMoveStack: ((CardStack, Offset, Offset, Int?, Int?) -> Unit)? = null,
    elevation: Dp = 4.dp
) {
    Box(modifier = Modifier.fillMaxSize()) {
        stack.cards.forEachIndexed { index, instance ->
            val finalX = if (stack.isSpread) (index * 38).dp else (if (index == 0) 0.dp else (10 + (index - 1) * 2).dp)
            val finalY = if (stack.isSpread) 0.dp else (if (index == 0) 0.dp else (10 + (index - 1) * 2).dp)

            Box(modifier = Modifier.offset(finalX, finalY)) {
                if (dragDropState != null && onMoveCard != null && onMoveStack != null) {
                    DraggableCard(
                        instance = instance,
                        zone = ZoneType.FIELD,
                        dragDropState = dragDropState,
                        onMove = onMoveCard,
                        showHandlers = false, 
                        stack = stack,
                        onMoveStack = onMoveStack,
                        elevation = elevation
                    )
                } else {
                    Box(modifier = Modifier.size(105.dp), contentAlignment = Alignment.Center) {
                        CardPlaceholder(instance.card.name, imgUrl = instance.card.imgUrl, isFlipped = instance.isFlipped, rotation = instance.rotation, elevation = elevation)
                    }
                }
            }
        }
    }
}

@Composable
fun DraggableCard(
    instance: CardInstance,
    zone: ZoneType,
    dragDropState: DragDropState,
    onMove: (CardInstance, ZoneType, ZoneType, Offset, Offset, Int?, Int?) -> Unit,
    modifier: Modifier = Modifier,
    showHandlers: Boolean = true,
    stack: CardStack? = null,
    onMoveStack: ((CardStack, Offset, Offset, Int?, Int?) -> Unit)? = null,
    onDrawCard: ((CardInstance) -> Unit)? = null,
    elevation: Dp = 4.dp,
    isPrivate: Boolean = false
) {
    var globalOffset by remember { mutableStateOf(Offset.Zero) }
    val isDragging = dragDropState.draggedCard?.id == instance.id
    val isHovered = dragDropState.hoverCardId == instance.id
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .size(110.dp)
        .onGloballyPositioned {
            globalOffset = it.positionInWindow() 
            if (zone == ZoneType.FIELD && stack == null) {
                dragDropState.updateCardRect(instance.id, it.boundsInWindow())
            }
        }
        .alpha(if (isDragging) 0.5f else 1f)
        .border(if (isHovered) 4.dp else 0.dp, Color.Yellow, MaterialTheme.shapes.small)
        .pointerInput(instance.id) {
            detectDragGestures(
                onDragStart = { localOffset ->
                    log("DRAG STARTING for card ${instance.id}")
                    dragDropState.draggedCard = instance
                    dragDropState.sourceZone = zone
                    dragDropState.grabOffset = localOffset
                    dragDropState.dragPosition = globalOffset + localOffset
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragDropState.dragPosition += dragAmount
                    dragDropState.hoverStackId = dragDropState.getTargetStack(dragDropState.dragPosition, stack?.id)
                    dragDropState.hoverCardId = dragDropState.getTargetCard(dragDropState.dragPosition, instance.id)
                },
                onDragEnd = {
                    log("DRAG ENDED for card ${instance.id}")
                    val targetZone = dragDropState.getTargetZone(dragDropState.dragPosition)
                    val targetStackId = dragDropState.getTargetStack(dragDropState.dragPosition, stack?.id)
                    val targetCardId = dragDropState.getTargetCard(dragDropState.dragPosition, instance.id)
                    if (targetZone != null) {
                        onMove(instance, zone, targetZone, dragDropState.dragPosition, dragDropState.grabOffset, targetStackId, targetCardId)
                    }
                    dragDropState.draggedCard = null
                    dragDropState.hoverStackId = null
                    dragDropState.hoverCardId = null
                }
            )
        }
        .clickable {
            log("TAP/CLICK on card ${instance.id}")
            showMenu = !showMenu
        },
        contentAlignment = Alignment.Center
    ) {
        CardPlaceholder(
            name = instance.card.name,
            imgUrl = instance.card.imgUrl,
            isFlipped = instance.isFlipped,
            isPrivate = isPrivate,
            rotation = instance.rotation,
            elevation = elevation
        )

        // Handlers Area
        if ((showHandlers || (showMenu && stack == null))) {
            Box(modifier = Modifier.fillMaxSize()) {
                key("F") { HandlerIcon("F", Color(0xFF4CAF50), Modifier.align(Alignment.TopStart).padding(2.dp)) { instance.isFlipped = !instance.isFlipped } }
                key("R") { HandlerIcon("R", Color(0xFFF44336), Modifier.align(Alignment.TopEnd).padding(2.dp)) {
                    instance.rotation = (instance.rotation + 90f) % 360f
                } }
            }
        }
    }
}

@Composable
fun HandlerIcon(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onDragStart: ((Offset) -> Unit)? = null,
    onDrag: ((Offset) -> Unit)? = null,
    onDragEnd: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    var windowPos by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier = modifier
            .size(36.dp) 
            .onGloballyPositioned { 
                windowPos = it.positionInWindow()
                val b = it.boundsInWindow()
                log("HandlerIcon " + label + " Bounds: L=" + b.left + ", T=" + b.top + ", R=" + b.right + ", B=" + b.bottom)
            }
            .background(color.copy(alpha = 0.9f), CircleShape)
            .border(1.5.dp, Color.White, CircleShape)
            .clickable {
                log("HandlerIcon " + label + " CLICKED (direct)")
                onClick?.invoke()
            }
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}

@Composable
fun CardPlaceholder(
    name: String, 
    imgUrl: String = "",
    modifier: Modifier = Modifier, 
    isFlipped: Boolean = false, 
    isPrivate: Boolean = false,
    rotation: Float = 0f,
    elevation: Dp = 4.dp
) {
    var imageBitmap by remember(imgUrl) { mutableStateOf<ImageBitmap?>(imageCache[imgUrl]) }
    
    LaunchedEffect(imgUrl) {
        if (imgUrl.isNotEmpty() && imageBitmap == null) {
            try {
                val bytes: ByteArray = globalHttpClient.get(imgUrl).body()
                val bitmap = org.jetbrains.skia.Image.makeFromEncoded(bytes).toComposeImageBitmap()
                imageCache[imgUrl] = bitmap
                imageBitmap = bitmap
            } catch (e: Exception) {
                log("Failed to load image: ${e.message}")
            }
        }
    }

    Surface(
        modifier = modifier
            .size(width = 75.dp, height = 105.dp)
            .rotate(rotation),
        color = if (isFlipped) Color(0xFF000080) else Color(0xFF3A3A3A),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFlipped && isPrivate) Color.Blue else Color.Gray),
        shadowElevation = elevation
    ) {
        log("CardPlaceholder drawing: name=$name isFlipped=$isFlipped")
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(4.dp)) {
            if (isFlipped) {
                Text("BS", color = Color.White, fontSize = 32.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            } else if (imageBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = imageBitmap!!,
                    contentDescription = name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            } else {
                Text(
                    text = name, 
                    color = if (isFlipped) Color.Gray else Color.White, 
                    style = MaterialTheme.typography.labelSmall, 
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, 
                    maxLines = 3
                )
            }
        }
    }
}

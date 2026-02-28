import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.*

val globalHttpClient = HttpClient {
    install(ContentNegotiation) {
        json()
    }
}

private val imageCache = mutableMapOf<String, ImageBitmap>()

class CardInstance(val id: Int, val card: SearchCard) {
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
    onMoveStack: (CardStack, Offset, Offset, Int?, Int?) -> Unit
) {
    var globalOffset by remember { mutableStateOf(Offset.Zero) }
    val isHovered = dragDropState.hoverStackId == stack.id
    val isDraggingStack = dragDropState.draggedStack?.id == stack.id

    // Square container large enough to hold rotated cards + icons
    val containerSize = 140.dp 

    Box(modifier = Modifier
        .offset { IntOffset(stack.offset.x.toInt(), stack.offset.y.toInt()) }
        .size(containerSize)
        .onGloballyPositioned { 
            globalOffset = it.positionInWindow()
            dragDropState.updateStackRect(stack.id, it.boundsInWindow()) 
        }
        .alpha(if (isDraggingStack) 0.5f else 1f)
        .border(if (isHovered) 4.dp else 0.dp, Color.Yellow, RoundedCornerShape(8.dp))
    ) {
        // Stack Background / Drag Layer
        Box(modifier = Modifier
            .fillMaxSize()
            .pointerInput(stack.id) {
                detectDragGestures(
                    onDragStart = { localOffset ->
                        dragDropState.draggedStack = stack
                        dragDropState.sourceZone = ZoneType.FIELD
                        dragDropState.grabOffset = localOffset
                        dragDropState.dragPosition = globalOffset + localOffset
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragDropState.dragPosition += dragAmount
                        dragDropState.hoverStackId = dragDropState.getTargetStack(dragDropState.dragPosition, stack.id)
                        dragDropState.hoverCardId = dragDropState.getTargetCard(dragDropState.dragPosition)
                    },
                    onDragEnd = {
                        val targetStackId = dragDropState.getTargetStack(dragDropState.dragPosition, stack.id)
                        val targetCardId = dragDropState.getTargetCard(dragDropState.dragPosition)
                        onMoveStack(stack, dragDropState.dragPosition, dragDropState.grabOffset, targetStackId, targetCardId)
                        dragDropState.draggedStack = null
                        dragDropState.hoverStackId = null
                        dragDropState.hoverCardId = null
                    }
                )
            }
        )

        // Cards in Stack
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            stack.cards.forEachIndexed { index, instance ->
                // Visual offset for stack effect
                val visualX = if (stack.isSpread) (index * 40).dp else (index * 2).dp
                val visualY = if (stack.isSpread) 0.dp else (index * 2).dp
                
                Box(modifier = Modifier.offset(visualX, visualY)) {
                    DraggableCard(
                        instance = instance,
                        zone = ZoneType.FIELD,
                        dragDropState = dragDropState,
                        onMove = onMoveCard,
                        showHandlers = false, // Individual handlers hidden in stack
                        stack = stack,
                        onMoveStack = onMoveStack
                    )
                }
            }
        }

        // Stack Controls (F, R, Spread)
        if (stack.cards.size > 1) {
            Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                HandlerIcon("F", Color(0xFF4CAF50), Modifier.align(Alignment.TopStart).zIndex(10f)) {
                    stack.cards.forEach { it.isFlipped = !it.isFlipped }
                }
                HandlerIcon("R", Color(0xFFF44336), Modifier.align(Alignment.TopEnd).zIndex(10f)) {
                    val newRot = ((stack.cards.lastOrNull()?.rotation ?: 0f) + 90f) % 360f
                    stack.cards.forEach { it.rotation = newRot }
                }
                HandlerIcon("E${stack.cards.size}", Color(0xFFE91E63), Modifier.align(Alignment.BottomEnd).zIndex(10f)) {
                    stack.isSpread = !stack.isSpread
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
    isPrivate: Boolean = false
) {
    var globalOffset by remember { mutableStateOf(Offset.Zero) }
    val isDragging = dragDropState.draggedCard?.id == instance.id
    val isHovered = dragDropState.hoverCardId == instance.id
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .size(130.dp) // Large enough for icons and card
        .onGloballyPositioned {
            globalOffset = it.positionInWindow() 
            if (zone == ZoneType.FIELD && stack == null) {
                dragDropState.updateCardRect(instance.id, it.boundsInWindow())
            }
        }
        .alpha(if (isDragging) 0.5f else 1f)
        .zIndex(if (isDragging) 100f else 0f)
    ) {
        // 1. Handlers Area (Z-Index high, placed FIRST or LAST to avoid overlap issues)
        if (showHandlers || (showMenu && stack == null)) {
            Box(modifier = Modifier.fillMaxSize().zIndex(20f)) {
                HandlerIcon("F", Color(0xFF4CAF50), Modifier.align(Alignment.TopStart)) {
                    instance.isFlipped = !instance.isFlipped
                }
                HandlerIcon("R", Color(0xFFF44336), Modifier.align(Alignment.TopEnd)) {
                    instance.rotation = (instance.rotation + 90f) % 360f
                }
            }
        }

        // 2. Card Body (Z-Index low)
        Box(modifier = Modifier
            .size(105.dp)
            .align(Alignment.Center)
            .rotate(instance.rotation)
            .border(if (isHovered) 4.dp else 0.dp, Color.Yellow, RoundedCornerShape(4.dp))
            .zIndex(1f)
            .pointerInput(instance.id) {
                detectDragGestures(
                    onDragStart = { localOffset ->
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
                if (stack == null) showMenu = !showMenu
            }
        ) {
            Box(modifier = Modifier.size(75.dp, 105.dp).align(Alignment.Center)) {
                CardPlaceholder(
                    name = instance.card.name,
                    imgUrl = instance.card.imgUrl,
                    isFlipped = instance.isFlipped,
                    isPrivate = isPrivate,
                    rotation = 0f
                )
            }
        }
    }
}

@Composable
fun HandlerIcon(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .size(48.dp) // Enlarged for better click success
            .pointerInput(label) {
                detectTapGestures {
                    log("Icon $label tapped (48dp area)")
                    onClick()
                }
            },
        shape = CircleShape,
        color = color.copy(alpha = 0.9f),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White),
        shadowElevation = 6.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CardPlaceholder(
    name: String, 
    imgUrl: String = "",
    isFlipped: Boolean = false, 
    isPrivate: Boolean = false,
    rotation: Float = 0f
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
        modifier = Modifier
            .fillMaxSize()
            .rotate(rotation),
        color = if (isFlipped) Color(0xFF1A237E) else Color(0xFF37474F), // Distinct dark blue for back
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isPrivate) Color.Cyan else Color.LightGray),
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(4.dp)) {
            if (isFlipped) {
                // Back Side
                Text("BS", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            } else {
                // Front Side
                if (imageBitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = imageBitmap!!,
                        contentDescription = name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = name, 
                        color = Color.White, 
                        style = MaterialTheme.typography.labelSmall, 
                        textAlign = TextAlign.Center, 
                        maxLines = 3
                    )
                }
            }
        }
    }
}

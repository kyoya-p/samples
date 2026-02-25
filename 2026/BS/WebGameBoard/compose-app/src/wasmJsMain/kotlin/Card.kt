import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val squareSize = maxOf(stackExtentX, stackExtentY, 105.dp) + 20.dp

    Box(modifier = Modifier
        .offset { IntOffset(stack.offset.x.toInt(), stack.offset.y.toInt()) }
        .size(squareSize)
        .onGloballyPositioned { 
            globalOffset = it.positionInWindow()
            dragDropState.updateStackRect(stack.id, it.boundsInWindow()) 
        }
        .alpha(if (isDraggingStack) 0f else 1f)
        .border(if (isHovered) 4.dp else 0.dp, Color.Yellow, MaterialTheme.shapes.small)
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
                },
                onDragCancel = { dragDropState.draggedStack = null; dragDropState.hoverStackId = null; dragDropState.hoverCardId = null }
            )
        }
    ) {
        CardStackContent(stack, dragDropState, onMoveCard, onMoveStack)
        
        if (stack.cards.size > 1) {
            Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                HandlerIcon("F", Color(0xFF4CAF50), Modifier.align(Alignment.TopStart)) {
                    stack.cards.lastOrNull()?.let { it.isFlipped = !it.isFlipped }
                }
                HandlerIcon("R", Color(0xFFF44336), Modifier.align(Alignment.TopEnd)) {
                    val newRot = ((stack.cards.lastOrNull()?.rotation ?: 0f) + 90f) % 360f
                    stack.cards.forEach { it.rotation = newRot }
                }
                HandlerIcon("M", Color(0xFF2196F3), Modifier.align(Alignment.BottomStart),
                    onDragStart = { absPos ->
                        val top = stack.cards.last()
                        dragDropState.draggedCard = top
                        dragDropState.sourceZone = ZoneType.FIELD
                        val staggerX = if(stack.isSpread) (spreadX.value * (stack.cards.size - 1)) else (10 + (stack.cards.size - 2) * 2).toFloat()
                        val staggerY = if(stack.isSpread) 0f else (10 + (stack.cards.size - 2) * 2).toFloat()
                        dragDropState.grabOffset = absPos - (globalOffset + Offset(staggerX, staggerY))
                        dragDropState.dragPosition = absPos
                    },
                    onDrag = { dragDropState.dragPosition += it },
                    onDragEnd = {
                        val targetZone = dragDropState.getTargetZone(dragDropState.dragPosition)
                        val tStack = dragDropState.getTargetStack(dragDropState.dragPosition, stack.id)
                        val tCard = dragDropState.getTargetCard(dragDropState.dragPosition)
                        onMoveCard(stack.cards.last(), ZoneType.FIELD, targetZone!!, dragDropState.dragPosition, dragDropState.grabOffset, tStack, tCard)
                        dragDropState.draggedCard = null
                    }
                )
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
                HandlerIcon("E${stack.cards.size}", Color(0xFFE91E63), Modifier.align(Alignment.BottomEnd).size(28.dp)) {
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
                        CardPlaceholder(instance.card.name, isFlipped = instance.isFlipped, rotation = instance.rotation, elevation = elevation)
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
    elevation: Dp = 4.dp,
    isPrivate: Boolean = false
) {
    var globalOffset by remember { mutableStateOf(Offset.Zero) }
    val isDragging = dragDropState.draggedCard?.id == instance.id
    val isHovered = dragDropState.hoverCardId == instance.id
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .size(105.dp)
        .onGloballyPositioned { 
            globalOffset = it.positionInWindow() 
            if (zone == ZoneType.FIELD && stack == null) {
                dragDropState.updateCardRect(instance.id, it.boundsInWindow())
            }
        }
        .alpha(if (isDragging) 0.5f else 1f)
        .border(if (isHovered) 4.dp else 0.dp, Color.Yellow, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center
    ) {
        CardPlaceholder(
            name = instance.card.name,
            isFlipped = instance.isFlipped,
            isPrivate = isPrivate,
            rotation = instance.rotation,
            elevation = elevation,
            modifier = Modifier.pointerInput(instance.id) {
                detectDragGestures(
                    onDragStart = { localOffset ->
                        dragDropState.draggedCard = instance
                        dragDropState.sourceZone = zone
                        dragDropState.grabOffset = localOffset + Offset(15f, 0f)
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
                    },
                    onDragCancel = { dragDropState.draggedCard = null; dragDropState.hoverStackId = null; dragDropState.hoverCardId = null }
                )
            }.clickable { showMenu = !showMenu }
        )

        if ((showHandlers || (showMenu && stack == null))) {
            Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                HandlerIcon("F", Color(0xFF4CAF50), Modifier.align(Alignment.TopStart)) { instance.isFlipped = !instance.isFlipped }
                HandlerIcon("R", Color(0xFFF44336), Modifier.align(Alignment.TopEnd)) {
                    instance.rotation = (instance.rotation + 90f) % 360f
                }
                HandlerIcon("M", Color(0xFF2196F3), Modifier.align(Alignment.BottomStart))
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
            .size(22.dp)
            .onGloballyPositioned { windowPos = it.positionInWindow() }
            .background(color.copy(alpha = 0.9f), CircleShape)
            .border(1.2.dp, Color.White, CircleShape)
            .then(
                if (onDragStart != null) {
                    Modifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { localOffset -> onDragStart(windowPos + localOffset) },
                            onDrag = { change, dragAmount -> 
                                change.consume()
                                onDrag?.invoke(dragAmount) 
                            },
                            onDragEnd = { onDragEnd?.invoke() },
                            onDragCancel = { onDragEnd?.invoke() }
                        )
                    }
                } else if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 9.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
    }
}

@Composable
fun CardPlaceholder(
    name: String, 
    modifier: Modifier = Modifier, 
    isFlipped: Boolean = false, 
    isPrivate: Boolean = false,
    rotation: Float = 0f,
    elevation: Dp = 4.dp
) {
    Surface(
        modifier = modifier
            .size(width = 75.dp, height = 105.dp)
            .rotate(rotation),
        color = if (isFlipped) Color(0xFF1A1A1A) else Color(0xFF3A3A3A),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isFlipped && isPrivate) Color.Blue else Color.Gray),
        shadowElevation = elevation
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(4.dp)) {
            if (isFlipped && !isPrivate) {
                Text("BS", color = Color.White, style = MaterialTheme.typography.labelLarge)
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

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
    onMoveCard: (CardInstance, ZoneType, ZoneType, Offset, Offset, Int?) -> Unit,
    onMoveStack: (CardStack, Offset, Offset, Int?) -> Unit
) {
    var globalOffset by remember { mutableStateOf(Offset.Zero) }
    val isHovered = dragDropState.hoverStackId == stack.id
    val isDraggingStack = dragDropState.draggedStack?.id == stack.id

    // Spread mode: 38dp (roughly half of 75dp card)
    val spreadOffset = 38.dp
    val width = if (stack.isSpread) (105.dp + spreadOffset * (stack.cards.size - 1)) else 125.dp
    val height = 125.dp

    Box(modifier = Modifier
        .offset { IntOffset(stack.offset.x.toInt(), stack.offset.y.toInt()) }
        .size(width = width, height = height)
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
                },
                onDragEnd = {
                    val targetStackId = dragDropState.getTargetStack(dragDropState.dragPosition, stack.id)
                    onMoveStack(stack, dragDropState.dragPosition, dragDropState.grabOffset, targetStackId)
                    dragDropState.draggedStack = null
                    dragDropState.hoverStackId = null
                },
                onDragCancel = { dragDropState.draggedStack = null; dragDropState.hoverStackId = null }
            )
        }
    ) {
        CardStackContent(stack, dragDropState, onMoveCard, onMoveStack, spreadOffset)
    }
}

@Composable
fun CardStackContent(
    stack: CardStack,
    dragDropState: DragDropState? = null,
    onMoveCard: ((CardInstance, ZoneType, ZoneType, Offset, Offset, Int?) -> Unit)? = null,
    onMoveStack: ((CardStack, Offset, Offset, Int?) -> Unit)? = null,
    spreadOffset: Dp = 38.dp,
    elevation: Dp = 4.dp
) {
    Box(modifier = Modifier.fillMaxSize()) {
        stack.cards.forEachIndexed { index, instance ->
            // Stagger: Bottom card 10dp, others 2dp.
            val staggerX = if (stack.isSpread) (spreadOffset * index) else (if (index == 0) 0.dp else (10 + (index - 1) * 2).dp)
            val staggerY = if (stack.isSpread) 0.dp else (if (index == 0) 0.dp else (10 + (index - 1) * 2).dp)

            Box(modifier = Modifier.offset(staggerX, staggerY)) {
                if (dragDropState != null && onMoveCard != null && onMoveStack != null) {
                    DraggableCard(
                        instance = instance,
                        zone = ZoneType.FIELD,
                        dragDropState = dragDropState,
                        onMove = onMoveCard,
                        showHandlers = index == stack.cards.size - 1,
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
    onMove: (CardInstance, ZoneType, ZoneType, Offset, Offset, Int?) -> Unit,
    modifier: Modifier = Modifier,
    showHandlers: Boolean = true,
    stack: CardStack? = null,
    onMoveStack: ((CardStack, Offset, Offset, Int?) -> Unit)? = null,
    elevation: Dp = 4.dp,
    isPrivate: Boolean = false
) {
    var globalOffset by remember { mutableStateOf(Offset.Zero) }
    val isDragging = dragDropState.draggedCard?.id == instance.id
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier
        .size(105.dp)
        .onGloballyPositioned { globalOffset = it.positionInWindow() }
        .alpha(if (isDragging) 0.5f else 1f),
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
                        dragDropState.hoverStackId = dragDropState.getTargetStack(dragDropState.dragPosition)
                    },
                    onDragEnd = {
                        val targetZone = dragDropState.getTargetZone(dragDropState.dragPosition)
                        val targetStackId = dragDropState.getTargetStack(dragDropState.dragPosition)
                        if (targetZone != null) {
                            onMove(instance, zone, targetZone, dragDropState.dragPosition, dragDropState.grabOffset, targetStackId)
                        }
                        dragDropState.draggedCard = null
                        dragDropState.hoverStackId = null
                    },
                    onDragCancel = { dragDropState.draggedCard = null; dragDropState.hoverStackId = null }
                )
            }.clickable { showMenu = !showMenu }
        )

        if ((showHandlers || showMenu)) {
            Box(modifier = Modifier.fillMaxSize().padding(2.dp)) {
                HandlerIcon("F", Color(0xFF4CAF50), Modifier.align(Alignment.TopStart)) { instance.isFlipped = !instance.isFlipped }
                HandlerIcon("R", Color(0xFFF44336), Modifier.align(Alignment.TopEnd)) {
                    val newRotation = (instance.rotation + 90f) % 360f
                    if (stack != null) stack.cards.forEach { it.rotation = newRotation }
                    else instance.rotation = newRotation
                }
                HandlerIcon("M", Color(0xFF2196F3), Modifier.align(Alignment.BottomStart))
                
                if (stack != null && stack.cards.size > 1) {
                    HandlerIcon(
                        label = "E${stack.cards.size}", 
                        color = Color(0xFFE91E63),
                        modifier = Modifier.align(Alignment.BottomEnd).size(26.dp)
                    ) {
                        stack.isSpread = !stack.isSpread
                    }
                }
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
                            onDrag = { change, dragAmount -> change.consume(); onDrag?.invoke(dragAmount) },
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
            // README: Private area confirms both sides (show name even if flipped)
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

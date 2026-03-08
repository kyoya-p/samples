package webrtc

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.CanvasBasedWindow
import kotlinx.coroutines.*
import kotlinx.browser.window
import org.w3c.dom.*
import kotlin.js.Date
import kotlin.math.*

class Dot(val xRel: Float, val yRel: Float, val color: Color, val startTime: Double)

class WebRTCController {
    private val scope = MainScope()
    private var peerConnection: dynamic = null
    private var dataChannel: dynamic = null
    private val room = "demo-room"
    private var socket: dynamic = null

    // State
    val localDots = mutableStateListOf<Dot>()
    val remoteDots = mutableStateListOf<Dot>()
    var isP2PConnected by mutableStateOf(false)
    var logs by mutableStateOf("")

    init {
        setupConsoleProxy()
        try {
            socket = js("io()")
            setupSocket()
        } catch (e: Exception) {
            println("Socket.io not found or failed to init")
        }
    }

    private fun setupConsoleProxy() {
        val controller = this
        js("""
            var originalLog = console.log;
            var originalError = console.error;
            console.log = function() {
                originalLog.apply(console, arguments);
                var argsArray = Array.prototype.slice.call(arguments);
                controller.appendLog('INFO', argsArray);
            };
            console.error = function() {
                originalError.apply(console, arguments);
                var argsArray = Array.prototype.slice.call(arguments);
                controller.appendLog('ERROR', argsArray);
            };
        """)
    }

    @JsName("appendLog")
    fun appendLog(type: String, args: Array<dynamic>) {
        val msg = args.map { arg ->
            if (js("typeof arg === 'object'")) JSON.stringify(arg) else arg.toString()
        }.joinToString(" ")
        logs += "[$type] $msg\n"
    }

    private fun setupSocket() {
        if (socket == null) return
        socket.emit("join", room)
        socket.on("signal", { data: dynamic ->
            handleSignal(data)
        })
    }

    private suspend fun setupP2P() {
        try {
            val response = window.fetch("/ice-servers").await()
            val iceServers = response.json().await()
            val config: dynamic = js("{}")
            config.iceServers = iceServers
            peerConnection = js("new RTCPeerConnection(config)")

            peerConnection.onicecandidate = { event: dynamic ->
                if (event.candidate != null) {
                    sendSignal(js("{type: 'candidate', candidate: event.candidate}"))
                }
            }
            peerConnection.ondatachannel = { event: dynamic ->
                setDataChannel(event.channel)
            }
        } catch (e: Exception) {
            println("Failed to setup P2P: ${e.message}")
        }
    }

    private fun setDataChannel(channel: dynamic) {
        dataChannel = channel
        dataChannel.onopen = { 
            println("DataChannel opened")
            isP2PConnected = true
        }
        dataChannel.onmessage = { event: dynamic ->
            val data = JSON.parse<dynamic>(event.data as String)
            if (data.type == "point") {
                drawRemotePoint(data.x.unsafeCast<Double>().toFloat(), data.y.unsafeCast<Double>().toFloat())
            }
        }
        dataChannel.onclose = {
            isP2PConnected = false
        }
    }

    fun connectP2P() {
        if (peerConnection == null) return
        dataChannel = peerConnection.createDataChannel("chaos-sync")
        setDataChannel(dataChannel)
        peerConnection.createOffer().then { offer: dynamic ->
            peerConnection.setLocalDescription(offer).then {
                sendSignal(offer)
            }
        }
    }

    private fun sendSignal(signal: dynamic) {
        if (socket == null) return
        signal.room = room
        socket.emit("signal", signal)
    }

    private fun handleSignal(data: dynamic) {
        scope.launch {
            if (peerConnection == null) return@launch
            when (data.type) {
                "offer" -> {
                    peerConnection.setRemoteDescription(js("new RTCSessionDescription(data)")).then {
                        peerConnection.createAnswer().then { answer: dynamic ->
                            peerConnection.setLocalDescription(answer).then {
                                sendSignal(answer)
                            }
                        }
                    }
                }
                "answer" -> {
                    peerConnection.setRemoteDescription(js("new RTCSessionDescription(data)"))
                }
                "candidate" -> {
                    peerConnection.addIceCandidate(js("new RTCIceCandidate(data.candidate)"))
                }
                "point" -> {
                    drawRemotePoint(data.x.unsafeCast<Double>().toFloat(), data.y.unsafeCast<Double>().toFloat())
                }
            }
        }
    }

    private fun drawRemotePoint(xRel: Float, yRel: Float) {
        remoteDots.add(Dot(xRel, yRel, Color(0xFFD83B01), Date.now()))
    }

    fun sendPoint(xRel: Float, yRel: Float) {
        val data = js("{type: 'point', x: xRel, y: yRel}")
        if (dataChannel != null && dataChannel.readyState == "open") {
            dataChannel.send(JSON.stringify(data))
        } else {
            sendSignal(data)
        }
    }

    fun cleanupOldDots(now: Double) {
        val FADE_DURATION = 60000.0
        localDots.removeAll { now - it.startTime >= FADE_DURATION }
        remoteDots.removeAll { now - it.startTime >= FADE_DURATION }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(controller: WebRTCController) {
    val scrollState = rememberScrollState()
    
    // Animation/Cleanup loop
    LaunchedEffect(Unit) {
        while(true) {
            controller.cleanupOldDots(Date.now())
            delay(100)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("WebRTC Click Sync (Compose UI)") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Controls Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = false, onCheckedChange = {})
                        Text(" Force TURN")
                    }
                    Button(
                        onClick = { controller.connectP2P() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (controller.isP2PConnected) Color(0xFF28a745) else MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (controller.isP2PConnected) "P2P Connected" else "Connect P2P")
                    }
                    OutlinedButton(onClick = {
                        controller.localDots.clear()
                        controller.remoteDots.clear()
                    }) {
                        Text("Clear All")
                    }
                }
            }

            // Local Draw Card
            DrawCard("Local (Drag to draw)", controller.localDots, Color(0xFF0078D4)) { x, y ->
                controller.localDots.add(Dot(x, y, Color(0xFF0078D4), Date.now()))
                controller.sendPoint(x, y)
            }

            // Remote Sync Card
            DrawCard("Remote (P2P Sync)", controller.remoteDots, Color(0xFFD83B01))

            // Logs Card
            Card(modifier = Modifier.fillMaxWidth().height(200.dp)) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text("Diagnostics Log", style = MaterialTheme.typography.titleSmall)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = controller.logs,
                        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun DrawCard(title: String, dots: List<Dot>, defaultColor: Color, onDraw: ((Float, Float) -> Unit)? = null) {
    Card(modifier = Modifier.fillMaxWidth().height(350.dp)) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .pointerInput(Unit) {
                        if (onDraw != null) {
                            detectDragGestures { change, _ ->
                                val x = change.position.x / size.width
                                val y = change.position.y / size.height
                                onDraw(x, y)
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val now = Date.now()
                    val FADE_DURATION = 60000.0
                    dots.forEach { dot ->
                        val alpha = 1.0f - ((now - dot.startTime) / FADE_DURATION).toFloat()
                        if (alpha > 0) {
                            drawCircle(
                                color = dot.color.copy(alpha = alpha),
                                radius = 10.dp.toPx(),
                                center = Offset(dot.xRel * size.width, dot.yRel * size.height)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val controller = WebRTCController()
    CanvasBasedWindow(title = "WebRTC Click Sync", canvasElementId = "app-canvas") {
        MaterialTheme {
            MainApp(controller)
        }
    }
}

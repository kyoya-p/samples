package webrtc

import kotlinx.coroutines.*
import kotlinx.browser.window
import kotlinx.browser.document
import org.w3c.dom.*
import org.w3c.dom.events.MouseEvent
import kotlin.js.Date
import kotlin.math.*

class Dot(val x: Double, val y: Double, val color: String, val startTime: Double)

class WebRTCController(val signalingUrl: String) {
    private val scope = MainScope()
    private var peerConnection: dynamic = null
    private var dataChannel: dynamic = null
    private val room = "demo-room"
    private var socket: dynamic = null

    private lateinit var localCanvas: HTMLCanvasElement
    private lateinit var remoteCanvas: HTMLCanvasElement
    private lateinit var localCtx: CanvasRenderingContext2D
    private lateinit var remoteCtx: CanvasRenderingContext2D
    private lateinit var logArea: HTMLTextAreaElement

    private var localDots = mutableListOf<Dot>()
    private var remoteDots = mutableListOf<Dot>()
    private var isDrawing = false

    init {
        buildUI()

        localCanvas = document.getElementById("localCanvas") as HTMLCanvasElement
        remoteCanvas = document.getElementById("remoteCanvas") as HTMLCanvasElement
        localCtx = localCanvas.getContext("2d") as CanvasRenderingContext2D
        remoteCtx = remoteCanvas.getContext("2d") as CanvasRenderingContext2D
        logArea = document.getElementById("logArea") as HTMLTextAreaElement

        setupConsoleProxy()
        initCanvases()
        setupEventListeners()
        
        // socket.ioの初期化
        try {
            socket = js("io()")
            setupSocket()
        } catch (e: Exception) {
            println("Socket.io not found or failed to init")
        }

        scope.launch {
            setupP2P()
        }
        animate()
        println("WebRTCController initialized with Kotlin logic and Dynamic UI")
    }

    private fun buildUI() {
        val app = document.getElementById("app") ?: document.body!!
        
        val container = document.createElement("div") as HTMLDivElement
        container.className = "container"
        
        // Controls
        val controls = document.createElement("div") as HTMLDivElement
        controls.className = "box-container controls"
        
        val label = document.createElement("label") as HTMLLabelElement
        val chkForceTurn = document.createElement("input") as HTMLInputElement
        chkForceTurn.type = "checkbox"
        chkForceTurn.id = "chkForceTurn"
        label.appendChild(chkForceTurn)
        label.appendChild(document.createTextNode(" Force TURN (Relay only)"))
        
        val btnConnect = document.createElement("button") as HTMLButtonElement
        btnConnect.id = "btnConnect"
        btnConnect.style.background = "#28a745"
        btnConnect.textContent = "Connect P2P (WebRTC)"
        
        val btnClear = document.createElement("button") as HTMLButtonElement
        btnClear.id = "btnClear"
        btnClear.style.background = "#666"
        btnClear.textContent = "Clear All"
        
        controls.appendChild(label)
        controls.appendChild(btnConnect)
        controls.appendChild(btnClear)
        container.appendChild(controls)
        
        // Local Canvas
        val localBox = document.createElement("div") as HTMLDivElement
        localBox.className = "box-container"
        val localH3 = document.createElement("h3") as HTMLElement
        localH3.textContent = "Local (Drag to draw)"
        val lc = document.createElement("canvas") as HTMLCanvasElement
        lc.id = "localCanvas"
        localBox.appendChild(localH3)
        localBox.appendChild(lc)
        container.appendChild(localBox)
        
        // Remote Canvas
        val remoteBox = document.createElement("div") as HTMLDivElement
        remoteBox.className = "box-container"
        val remoteH3 = document.createElement("h3") as HTMLElement
        remoteH3.textContent = "Remote (P2P Sync)"
        val rc = document.createElement("canvas") as HTMLCanvasElement
        rc.id = "remoteCanvas"
        remoteBox.appendChild(remoteH3)
        remoteBox.appendChild(rc)
        container.appendChild(remoteBox)
        
        // Log Area
        val logBox = document.createElement("div") as HTMLDivElement
        logBox.className = "box-container"
        val logH3 = document.createElement("h3") as HTMLElement
        logH3.textContent = "Diagnostics Log"
        val txtLog = document.createElement("textarea") as HTMLTextAreaElement
        txtLog.id = "logArea"
        txtLog.readOnly = true
        txtLog.placeholder = "Logs will appear here..."
        logBox.appendChild(logH3)
        logBox.appendChild(txtLog)
        container.appendChild(logBox)
        
        app.appendChild(container)
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
        logArea.value += "[$type] $msg\n"
        logArea.scrollTop = logArea.scrollHeight.toDouble()
    }

    private fun initCanvases() {
        fun resize(canvas: HTMLCanvasElement) {
            val rect = canvas.getBoundingClientRect()
            canvas.width = rect.width.toInt()
            if (canvas.width == 0) canvas.width = 400
            canvas.height = 350
        }
        resize(localCanvas)
        resize(remoteCanvas)
        window.addEventListener("resize", {
            resize(localCanvas)
            resize(remoteCanvas)
        })
    }

    private fun setupEventListeners() {
        document.getElementById("btnConnect")?.addEventListener("click", { connectP2P() })
        document.getElementById("btnClear")?.addEventListener("click", {
            localDots.clear()
            remoteDots.clear()
        })

        val handleMove = { x: Double, y: Double ->
            if (isDrawing) {
                val rect = localCanvas.getBoundingClientRect()
                val xAbs = x - rect.left
                val yAbs = y - rect.top
                val xRel = xAbs / rect.width
                val yRel = yAbs / rect.height

                localDots.add(Dot(xAbs, yAbs, "#0078d4", Date.now()))
                sendPoint(xRel, yRel)
            }
        }

        localCanvas.addEventListener("mousedown", { e ->
            val me = e as MouseEvent
            isDrawing = true
            handleMove(me.clientX.toDouble(), me.clientY.toDouble())
        })
        window.addEventListener("mouseup", { isDrawing = false })
        localCanvas.addEventListener("mousemove", { e ->
            val me = e as MouseEvent
            handleMove(me.clientX.toDouble(), me.clientY.toDouble())
        })

        localCanvas.addEventListener("touchstart", { e ->
            val te = e.asDynamic()
            if (te.touches.length > 0) {
                isDrawing = true
                val t = te.touches[0]
                handleMove(t.clientX as Double, t.clientY as Double)
            }
            e.preventDefault()
        })
        localCanvas.addEventListener("touchmove", { e ->
            val te = e.asDynamic()
            if (isDrawing && te.touches.length > 0) {
                val t = te.touches[0]
                handleMove(t.clientX as Double, t.clientY as Double)
            }
            e.preventDefault()
        })
        localCanvas.addEventListener("touchend", { isDrawing = false })
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
        dataChannel.onopen = { println("DataChannel opened") }
        dataChannel.onmessage = { event: dynamic ->
            val data = JSON.parse<dynamic>(event.data as String)
            if (data.type == "point") {
                drawRemotePoint(data.x as Double, data.y as Double)
            }
        }
    }

    private fun connectP2P() {
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
                    drawRemotePoint(data.x as Double, data.y as Double)
                }
            }
        }
    }

    private fun drawRemotePoint(xRel: Double, yRel: Double) {
        val xAbs = xRel * remoteCanvas.width
        val yAbs = yRel * remoteCanvas.height
        remoteDots.add(Dot(xAbs, yAbs, "#d83b01", Date.now()))
    }

    private fun sendPoint(xRel: Double, yRel: Double) {
        val data = js("{type: 'point', x: xRel, y: yRel}")
        if (dataChannel != null && dataChannel.readyState == "open") {
            dataChannel.send(JSON.stringify(data))
        } else {
            sendSignal(data)
        }
    }

    private fun animate() {
        val now = Date.now()
        val FADE_DURATION = 60000.0

        fun drawSet(ctx: CanvasRenderingContext2D, canvas: HTMLCanvasElement, dots: MutableList<Dot>) {
            ctx.clearRect(0.0, 0.0, canvas.width.toDouble(), canvas.height.toDouble())
            val iterator = dots.iterator()
            while (iterator.hasNext()) {
                val dot = iterator.next()
                val elapsed = now - dot.startTime
                if (elapsed >= FADE_DURATION) {
                    iterator.remove()
                    continue
                }
                val alpha = 1.0 - (elapsed / FADE_DURATION)
                ctx.globalAlpha = alpha
                ctx.fillStyle = dot.color
                ctx.beginPath()
                ctx.arc(dot.x, dot.y, 10.0, 0.0, PI * 2)
                ctx.fill()
            }
        }

        drawSet(localCtx, localCanvas, localDots)
        drawSet(remoteCtx, remoteCanvas, remoteDots)

        window.requestAnimationFrame { animate() }
    }
}

fun main() {
    window.onload = {
        WebRTCController(window.location.origin)
    }
}

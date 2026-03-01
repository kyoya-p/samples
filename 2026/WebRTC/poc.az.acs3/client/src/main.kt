package webrtc

import kotlinx.coroutines.*
import kotlinx.browser.window
import org.w3c.dom.url.URL
import kotlin.js.JsExport
import kotlin.js.JsName
import kotlin.math.*

@JsExport
class WebRTCController(val signalingUrl: String) {
    private val scope = MainScope()
    private var chaosJob: Job? = null
    private var currentHue = 0.0
    private var x = 0.5 
    private val r = 3.9

    private var peerConnection: dynamic = null
    private var dataChannel: dynamic = null
    private val room = "demo-room"

    init {
        startListening()
        scope.launch {
            setupP2P()
        }
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
            handleRemoteData(data)
        }
    }

    @JsName("connectP2P")
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
        signal.room = room
        window.asDynamic().emitSignal(signal)
    }

    private fun handleRemoteData(data: dynamic) {
        if (data.type == "color") {
            window.asDynamic().updateRemoteColorUI(data.value)
        } else if (data.type == "point") {
            window.asDynamic().drawRemotePoint(data.x, data.y)
        }
    }

    @JsName("sendPoint")
    fun sendPoint(x: Double, y: Double) {
        val data = js("{type: 'point', x: x, y: y}")
        if (dataChannel != null && dataChannel.readyState == "open") {
            dataChannel.send(JSON.stringify(data))
        } else {
            // Fallback to signaling if P2P not ready
            window.asDynamic().emitSignal(data)
        }
    }

    @JsName("startChaosStream")
    fun startChaosStream(onLocalColor: (String) -> Unit) {
        chaosJob?.cancel()
        chaosJob = scope.launch {
            while (isActive) {
                x = r * x * (1.0 - x)
                val delta = (x - 0.5) * 2.0 * (360.0 / 30.0)
                currentHue = (currentHue + delta + 360.0) % 360.0
                val rgb = hslToHex(currentHue, 0.8, 0.5)
                onLocalColor(rgb)

                val data = js("{type: 'color', value: rgb}")
                if (dataChannel != null && dataChannel.readyState == "open") {
                    dataChannel.send(JSON.stringify(data))
                } else {
                    window.asDynamic().emitSignal(data)
                }
                delay(300)
            }
        }
    }

    private fun startListening() {
        window.asDynamic().onRemoteSignal = { data: dynamic ->
            scope.launch {
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
                    else -> handleRemoteData(data)
                }
            }
        }
    }

    private fun hslToHex(h: Double, s: Double, l: Double): String {
        val c = (1.0 - abs(2.0 * l - 1.0)) * s
        val xPrime = c * (1.0 - abs((h / 60.0) % 2.0 - 1.0))
        val m = l - c / 2.0
        val (r, g, b) = when {
            h < 60 -> Triple(c, xPrime, 0.0)
            h < 120 -> Triple(xPrime, c, 0.0)
            h < 180 -> Triple(0.0, c, xPrime)
            h < 240 -> Triple(0.0, xPrime, c)
            h < 300 -> Triple(xPrime, 0.0, c)
            else -> Triple(c, 0.0, xPrime)
        }
        val rf = ((r + m) * 255).roundToInt()
        val gf = ((g + m) * 255).roundToInt()
        val bf = ((b + m) * 255).roundToInt()
        return "#${rf.toString(16).padStart(2, '0')}${gf.toString(16).padStart(2, '0')}${bf.toString(16).padStart(2, '0')}".uppercase()
    }
}

fun main() {
    val controller = WebRTCController(window.location.origin)
    window.asDynamic().webrtcController = controller
}
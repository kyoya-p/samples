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
        if (data.type == "point") {
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
}

fun main() {
    val controller = WebRTCController(window.location.origin)
    window.asDynamic().webrtcController = controller
}

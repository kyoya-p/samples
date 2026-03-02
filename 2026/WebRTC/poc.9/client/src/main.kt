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
    private var iceServers: dynamic = null
    private val room = "demo-room"

    init {
        startListening()
        scope.launch {
            fetchIceServers()
        }
    }

    private fun isForceTurn(): Boolean {
        val el = window.document.getElementById("chkForceTurn")
        return if (el != null) el.asDynamic().checked as Boolean else false
    }

    private suspend fun fetchIceServers() {
        try {
            val response = window.fetch("/ice-servers").await()
            iceServers = response.json().await()
            println("ICE Servers fetched successfully.")
        } catch (e: Exception) {
            println("Failed to fetch ICE servers: ${e.message}")
        }
    }

    private fun createPeerConnection() {
        if (iceServers == null) {
            println("Error: ICE servers not loaded yet.")
            return
        }

        val config: dynamic = js("{}")
        config.iceServers = iceServers
        
        if (isForceTurn()) {
            println("Policy: Force TURN (Relay only)")
            config.iceTransportPolicy = "relay"
        } else {
            println("Policy: All (Allow host/srflx)")
        }

        peerConnection = js("new RTCPeerConnection(config)")

        peerConnection.onicecandidate = { event: dynamic ->
            if (event.candidate != null) {
                val cand = event.candidate
                println("Gathered ICE Candidate: type=${cand.type} protocol=${cand.protocol} address=${cand.address}:${cand.port}")
                sendSignal(js("{type: 'candidate', candidate: event.candidate}"))
            } else {
                println("ICE Gathering complete.")
            }
        }

        peerConnection.ondatachannel = { event: dynamic ->
            println("DataChannel received from remote")
            setDataChannel(event.channel)
        }

        peerConnection.onconnectionstatechange = { _: dynamic ->
            println("WebRTC ConnectionState: ${peerConnection.connectionState}")
        }

        peerConnection.oniceconnectionstatechange = { _: dynamic ->
            println("WebRTC ICEConnectionState: ${peerConnection.iceConnectionState}")
        }

        peerConnection.onsignalingstatechange = { _: dynamic ->
            println("WebRTC SignalingState: ${peerConnection.signalingState}")
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
        createPeerConnection()
        if (peerConnection == null) return

        dataChannel = peerConnection.createDataChannel("chaos-sync")
        setDataChannel(dataChannel)

        peerConnection.createOffer().then { offer: dynamic ->
            peerConnection.setLocalDescription(offer).then {
                println("Local Offer set")
                sendSignal(offer)
            }
        }
    }

    private fun sendSignal(signal: dynamic) {
        if (signal.type == "offer" || signal.type == "answer") {
            println("Sending signal: ${signal.type}")
        }
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
                        println("Received Remote Offer")
                        if (peerConnection == null) {
                            createPeerConnection()
                        }
                        peerConnection.setRemoteDescription(js("new RTCSessionDescription(data)")).then {
                            peerConnection.createAnswer().then { answer: dynamic ->
                                peerConnection.setLocalDescription(answer).then {
                                    println("Local Answer set")
                                    sendSignal(answer)
                                }
                            }
                        }
                    }
                    "answer" -> {
                        println("Received Remote Answer")
                        peerConnection.setRemoteDescription(js("new RTCSessionDescription(data)"))
                    }
                    "candidate" -> {
                        if (peerConnection != null) {
                            peerConnection.addIceCandidate(js("new RTCIceCandidate(data.candidate)"))
                        }
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

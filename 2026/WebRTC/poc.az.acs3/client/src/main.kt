package webrtc

import kotlinx.coroutines.*
import kotlinx.browser.window
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

    init {
        startListening()
    }

    @JsName("sendPoint")
    fun sendPoint(x: Double, y: Double) {
        // 座標データをシグナリング経由で送信
        window.asDynamic().emitPoint(x, y)
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
                window.asDynamic().emitColor(rgb)
                delay(300)
            }
        }
    }

    private fun startListening() {
        window.asDynamic().onRemoteData = { data: dynamic ->
            if (data.type == "color") {
                window.asDynamic().updateRemoteColorUI(data.value)
            } else if (data.type == "point") {
                window.asDynamic().drawRemotePoint(data.x, data.y)
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
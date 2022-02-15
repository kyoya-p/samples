import org.w3c.dom.Window

data class AesAlgorithmKeyGen(val name: String, val length: Int)
data class AesAlgorithmEncrypt(val name: String, val iv: ByteArray)

val Window.crypto: dynamic
    get() {
        @Suppress("UNUSED_VARIABLE") val t = this
        return js("t.crypto")
    }

fun TextEncoder(@Suppress("UNUSED_PARAMETER") encoding: String) = js("new TextEncoder(encoding)")


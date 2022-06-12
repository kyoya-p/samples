package jp.wjg.shokkaa

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.promise
import org.w3c.dom.Window
import kotlin.js.Promise

data class AesAlgorithmKeyGen(val name: String, val length: Int)
data class AesAlgorithmEncrypt(val name: String, val iv: ByteArray)
class Crypto(val rawCrypto: dynamic) {
    val subtle: SubtleCrypto
        get() = SubtleCrypto(rawCrypto.subtle)
}

class SubtleCrypto(val rawSubtleCrypto: dynamic) {
    fun generateKeyX(algorithm: AesAlgorithmKeyGen, extractable: Boolean, keyUsages: List<String>): Promise<Any> =
        GlobalScope.promise {
            rawSubtleCrypto.generateKey(algorithm, extractable, keyUsages).then { k -> return@then k }
        }
//        Promise { resolve, rej -> (raw.generateKey(algorithm, extractable, keyUsages)) }

}

val Window.crypto: Crypto
    get() {
        @Suppress("UNUSED_VARIABLE") val t = this
        return Crypto(js("t.crypto"))
    }

fun TextEncoder(@Suppress("UNUSED_PARAMETER") encoding: String) = js("new TextEncoder(encoding)")


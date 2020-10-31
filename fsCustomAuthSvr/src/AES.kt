package AesEncriptor

import java.io.File
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("usage: aesKt srcfile keystring")
        System.exit(-1)
    }
    val fn = args[0]
    val key = args[1]
    val src = File(fn).readText()
    val enc = src.encript(key)
    println(enc)
}

// mode: Cipher.ENCRYPT_MODE | Cipher.DECRYPT_MODE
// key: ByteArrayに変換し16バイトで切る。 足りない分は0で埋める
fun ByteArray.cript(mode: Int, keyStr: ByteArray): ByteArray {
    val keySpec = SecretKeySpec((keyStr + ByteArray(16) { 0 }).take(16).toByteArray(), "AES")
    val iv = IvParameterSpec("0123456789abcdef".toByteArray())
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(mode, keySpec, iv)
    return cipher.doFinal(this)
}

fun solt() = Random.Default.nextBytes(8)
fun String.encript(key: String) = toByteArray(Charsets.UTF_8).let { solt() + it }.cript(Cipher.ENCRYPT_MODE, key.toByteArray()).let { String(Base64.getEncoder().encode(it), Charsets.UTF_8) }
fun String.decript(key: String) = Base64.getDecoder().decode(this).cript(Cipher.DECRYPT_MODE, key.toByteArray()).drop(8).let { String(it.toByteArray(), Charsets.UTF_8) }
fun ByteArray.encript(key: ByteArray) = ( solt() + this ).cript(Cipher.ENCRYPT_MODE, key).let { Base64.getEncoder().encode(it).toList() }
fun ByteArray.decript(key: ByteArray) = Base64.getDecoder().decode(this).cript(Cipher.DECRYPT_MODE, key).drop(8)

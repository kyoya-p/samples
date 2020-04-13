import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

fun main() {
    val enc = "abcdefg".toByteArray(Charsets.UTF_8).encript("0123456789abcdef")
    val dec = enc.decript("0123456789abcdef").let { String(it, Charsets.UTF_8) }

    println(dec)
}

fun ByteArray.encript(key: String): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(), "AES"))
    return cipher.doFinal(this)
}

fun ByteArray.decript(key: String): ByteArray {
    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key.toByteArray(), "AES"))
    return cipher.doFinal(this)
}

import jp.wjg.shokkaa.AesAlgorithmEncrypt
import jp.wjg.shokkaa.AesAlgorithmKeyGen
import jp.wjg.shokkaa.crypto
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.await
import org.w3c.fetch.CORS
import org.w3c.fetch.RequestInit
import org.w3c.fetch.RequestMode


const val cryptJs = "https://cdnjs.cloudflare.com/ajax/libs/crypto-js/3.1.9-1/crypto-js.min.js"

fun main() {
    val aesAlgorithmKeyGen = AesAlgorithmKeyGen(name = "AES-CBC", length = 128)
    val aesAlgorithmEncrypt = AesAlgorithmEncrypt(name = "AES-CBC", iv = ByteArray(16))


    window.crypto.subtle.generateKeyX(aesAlgorithmKeyGen, false, listOf("encrypt", "decrypt"))
        .then { k -> window.alert("$k") }
    window.alert(">>>end")


//    val encoder = TextEncoder("utf-8")
//    val clearDataArrayBufferView = encoder.encode("Plain Text Data")
//    window.crypto.subtle.generateKey(aesAlgorithmKeyGen, false, listOf("encrypt"))
//    window.alert(clearDataArrayBufferView.toString())

//    val key = js("""
//window.crypto.subtle.generateKey(
//    {
//        name: "AES-CBC",
//        length: 256, //can be  128, 192, or 256
//    },
//    false, //whether the key is extractable (i.e. can be used in exportKey)
//    ["encrypt", "decrypt"] //can be "encrypt", "decrypt", "wrapKey", or "unwrapKey"
//)
//.then(function(key){
//    //returns a key object
//    console.log(key);
//    window.alert(key);
//})
//.catch(function(err){
//    console.error(err);
//});
//    """)
//    window.alert("$key")
//
//    js(
//        """
//window.crypto.subtle.encrypt(
//    {
//        name: "AES-CBC",
//        //Don't re-use initialization vectors!
//        //Always generate a new iv every time your encrypt!
//        iv: window.crypto.getRandomValues(new Uint8Array(16)),
//    },
//    key, //from generateKey or importKey above
//    data //ArrayBuffer of data you want to encrypt
//)
//.then(function(encrypted){
//    //returns an ArrayBuffer containing the encrypted data
//    console.log(new Uint8Array(encrypted));
//})
//.catch(function(err){
//    console.error(err);
//});
//
//    """
//    )


}

@Suppress("unused")
suspend fun load(url: String) = window.fetch(
    url,
    RequestInit(
        //headers = Headers().apply { set("Access-Control-Allow-Origin", url) },
        mode = RequestMode.CORS,
    ),
).await().text().await()

@Suppress("unused")
fun function(@Suppress("UNUSED_PARAMETER") jsCode: String) = js("window.Function(jsCode)")
//fun requireJsModule(module: String) = js("require(module)")

@Suppress("unused")
fun loadingLib(path: String) = document.createElement("script").apply {
    setAttribute("src", path)
    setAttribute("type", "javascript")
}

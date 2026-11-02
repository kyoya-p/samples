import java.io.File
import java.security.KeyStore

// store file は mise run gen-stores (keytool) で生成する。環境変数で場所とパスワードを指定できる
//   KEYSTORE_PATH   : サーバの keystore (鍵 + 証明書)。未設定なら build/storefiles/keystore.p12
//   KEYSTORE_PASS   : keystore のパスワード (PKCS12 は鍵も同じ)。未設定なら 123456
//   TRUSTSTORE_PATH : クライアントの truststore (CA 証明書)。未設定なら build/storefiles/truststore.p12
//   TRUSTSTORE_PASS : truststore のパスワード。未設定なら 123456
// mise.toml の [env] で既定値を設定している
private fun env(name: String, default: String) = System.getenv(name)?.takeIf { it.isNotBlank() } ?: default

val KEYSTORE_PATH: String = env("KEYSTORE_PATH", "build/storefiles/keystore.p12")
val KEYSTORE_PASS: String = env("KEYSTORE_PASS", "123456")
val TRUSTSTORE_PATH: String = env("TRUSTSTORE_PATH", "build/storefiles/truststore.p12")
val TRUSTSTORE_PASS: String = env("TRUSTSTORE_PASS", "123456")

const val SERVER_ALIAS = "sampleAlias"

// PKCS12 の store file を開く
fun loadStore(path: String, pass: String): KeyStore {
    val file = File(path).takeIf { it.isFile }
        ?: error("store file が見つかりません: $path。先に mise run gen-stores を実行してください")
    return file.inputStream().use { KeyStore.getInstance("PKCS12").apply { load(it, pass.toCharArray()) } }
}

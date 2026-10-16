import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.ByteArrayInputStream
import java.io.File
import java.security.KeyFactory
import java.security.KeyStore
import java.security.cert.CertificateFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

// 証明書・秘密鍵をソースに内蔵したサーバ (ファイル不要。server_storefile.kt と同じポート 8080/8443 を使うため同時起動不可)
//
// 警告: 以下の秘密鍵はサンプル用に公開している。このサンプル以外では絶対に使わないこと
// CA 証明書 (EMBEDDED_CA_PEM) は、起動時に TRUSTSTORE_PATH と同じディレクトリの truststore_embedded.p12
// (パスワードは TRUSTSTORE_PASS) へ書き出す。クライアントは
//   .\kotlin.bat run --main-class=Client_secureKt -- build/storefiles/truststore_embedded.p12
// のように、このファイルを参照して検証する (先にこのサーバを起動しておく)。有効期限は生成から10年 (2036-10)

private const val EMBEDDED_CA_PEM = """
-----BEGIN CERTIFICATE-----
MIICJjCCAY+gAwIBAgIIYgOiStXetRYwDQYJKoZIhvcNAQEFBQAwSjELMAkGA1UE
BhMCSlAxDDAKBgNVBAoTA09yZzEQMA4GA1UECxMHT3JnVW5pdDEbMBkGA1UEAxMS
U2FtcGxlIEVtYmVkZGVkIENBMCAXDTI2MTAxNjIxMDAyMloYDzIwMzYxMDEzMjEw
MDIyWjBKMQswCQYDVQQGEwJKUDEMMAoGA1UEChMDT3JnMRAwDgYDVQQLEwdPcmdV
bml0MRswGQYDVQQDExJTYW1wbGUgRW1iZWRkZWQgQ0EwgZ8wDQYJKoZIhvcNAQEB
BQADgY0AMIGJAoGBAMZu+s3c3ZY+Uiki3iIWzGXPS+s3ktnKjgaKHXdbttVsYjad
e9yMoTb78sIZpQ2Ft+FnUPGOnrS6GD2/CGbYMhCScTIBUrBrXXZu+/tWVI1NI+cG
mASxYU9SZa9gyZ6lh6zkP2y6Sb1sUCxRBle7HoRMX2uitMYE3blHaJ48AHIvAgMB
AAGjEzARMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQEFBQADgYEAWu6SJr/Y
hPKuTaevAc5udP9rt4jLfntmWOUTx6KuizZzgjXad0RxoUGyXwk0P8TbIUo/E9l8
lWRCj4zFpZOsUNnHpn5nxrIpfyRvgzRHXnj2vvkbSOV017bTjSZsVhIxmol0VYqM
6//Cd9RpUL+4vlH4S17eo65bORcjWr3HUP8=
-----END CERTIFICATE-----
"""

private const val EMBEDDED_SERVER_CERT_PEM = """
-----BEGIN CERTIFICATE-----
MIICUTCCAbqgAwIBAgIIfm37JsHNo/QwDQYJKoZIhvcNAQEFBQAwSjELMAkGA1UE
BhMCSlAxDDAKBgNVBAoTA09yZzEQMA4GA1UECxMHT3JnVW5pdDEbMBkGA1UEAxMS
U2FtcGxlIEVtYmVkZGVkIENBMCAXDTI2MTAxNjIxMDAyMloYDzIwMzYxMDEzMjEw
MDIyWjBBMQswCQYDVQQGEwJKUDEMMAoGA1UEChMDT3JnMRAwDgYDVQQLEwdPcmdV
bml0MRIwEAYDVQQDEwkxMjcuMC4wLjEwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJ
AoGBAJeP2N1DYl14KxN2wMuMkKe9XSgN7ZIfqr54x1QxciYicCCm2kWa0zm2AZvg
8J5cY/Kwi3qsflVA1e6X1Od0lxEn3xad1zfMT0f8bn00X2gWwZZzt5HyLNlz7Q1X
68X5XMq4yZ9900/OfloG01nDL/K4IsMrVrtZEZsl7cA1vOeDAgMBAAGjRzBFMBMG
A1UdJQQMMAoGCCsGAQUFBwMBMC4GA1UdEQQnMCWCCTEyNy4wLjAuMYIHMC4wLjAu
MIIJbG9jYWxob3N0hwR/AAABMA0GCSqGSIb3DQEBBQUAA4GBABAk/29K7RfY/giB
cd8canqnYYi2N+KG3NoMlLwzQSevxbLoMyqzggvjF3gEEIeevAgD3oMGXzjiTXdr
yjzHw5qvPo4vChR6X4t1dWEL3/pyotFS4hPKuZihK9hlNdy7GimLbJvoqZPSVM+V
mVzsfHdlYXaCFCH5CJB2K4vvwAsb
-----END CERTIFICATE-----
"""

private const val EMBEDDED_SERVER_KEY_PEM = """
-----BEGIN PRIVATE KEY-----
MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAJeP2N1DYl14KxN2
wMuMkKe9XSgN7ZIfqr54x1QxciYicCCm2kWa0zm2AZvg8J5cY/Kwi3qsflVA1e6X
1Od0lxEn3xad1zfMT0f8bn00X2gWwZZzt5HyLNlz7Q1X68X5XMq4yZ9900/OfloG
01nDL/K4IsMrVrtZEZsl7cA1vOeDAgMBAAECgYAC7+Ct+P5Q/WEoUWbPCvxKo9rI
SezOphpy8ng6Y3Lo1kYB+/nC24s4VRSW5rcC+ptdOpqZV/piwIzkwMB5c8LxpEwU
n99R6mw2kS73v0pjo9iN+WhaUb8kXtNcWpzsGTqsbfl5pgB97PX2jb6ktbmC1ub5
+MJe5RaivYRjGkkFMQJBAJnv1zotis3oM8JS4lMYJQOu4T7wb8XNXqHiTGOD+kNd
jlqkMogrd7gTYKxX0+DMltEANHdLwNhZPdEna6d21J0CQQD8DOUX4cGbM63y2Bdm
sYGD6m3bNnUzvPykIxurLZlJZOX2jlIN0PreZwpYObfM+gXy3QoezvqcodKd67LB
2iKfAkAMTaUQG0+6wVJRt7orFgTvVGzgUOs+DfoASkv8sDGybKnSCMmZJJA/AB17
6NEgaHmtXqzS1BUntYpBSztJeS/pAkEA+PhD07NMauJBkP+jHe144bVPjpnca57i
b7++NPh10em4vjMXGk6zf7yIm11oETFnbt6ogIYzR0URpqyLhrDSQQJAIRjRibnJ
m/GEqQ0pYCGefnzYC9fegt01z7JoDymyPR9kd9rlEDdBzvebxp3bGflpxrKMJ15a
ImeVSP2Wxr7xlg==
-----END PRIVATE KEY-----
"""

private fun pemBody(pem: String) =
    Base64.getMimeDecoder().decode(pem.lines().filter { it.isNotBlank() && !it.startsWith("-----") }.joinToString(""))

// 内蔵 PEM から、サーバ証明書 + CA の証明書チェーンと秘密鍵を持つ KeyStore をメモリ上に作る
fun buildEmbeddedKeyStore(): KeyStore {
    val cf = CertificateFactory.getInstance("X.509")
    fun cert(pem: String) = cf.generateCertificate(ByteArrayInputStream(pemBody(pem)))
    val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(pemBody(EMBEDDED_SERVER_KEY_PEM)))
    return KeyStore.getInstance("PKCS12").apply {
        load(null, null)
        setKeyEntry(
            SERVER_ALIAS, key, KEYSTORE_PASS.toCharArray(),
            arrayOf(cert(EMBEDDED_SERVER_CERT_PEM), cert(EMBEDDED_CA_PEM))
        )
    }
}

fun main() {
    // クライアント (client_secure) が参照できるよう、CA 証明書を truststore に書き出す
    val trustFile = File(File(TRUSTSTORE_PATH).absoluteFile.parentFile, "truststore_embedded.p12")
    trustFile.parentFile.mkdirs()
    val ca = CertificateFactory.getInstance("X.509").generateCertificate(ByteArrayInputStream(pemBody(EMBEDDED_CA_PEM)))
    val trustStore = KeyStore.getInstance("PKCS12").apply {
        load(null, null)
        setCertificateEntry("sampleEmbeddedCA", ca)
    }
    trustFile.outputStream().use { trustStore.store(it, TRUSTSTORE_PASS.toCharArray()) }
    println("truststore 書き出し: $trustFile")
    val keyStore = buildEmbeddedKeyStore()
    embeddedServer(Netty, configure = {
        sslConnector(
            keyStore = keyStore,
            keyAlias = SERVER_ALIAS,
            keyStorePassword = { KEYSTORE_PASS.toCharArray() },
            privateKeyPassword = { KEYSTORE_PASS.toCharArray() }
        ) { port = 8443 }
        connector { port = 8080 } // 非SSL用ポート
    }) { myApplication() }.start(wait = true)
}

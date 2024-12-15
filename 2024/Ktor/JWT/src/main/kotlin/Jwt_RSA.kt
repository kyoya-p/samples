import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.io.File
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


@OptIn(ExperimentalEncodingApi::class)
fun main() {
    val privKey = File("secrets/private_key_pk8.der").readBytes()
    val pubLKey = File("src/main/resources/public_key_pk8.der").readBytes()

    val jwkProvider = JwkProviderBuilder("")
//        .cached(10, 24, TimeUnit.HOURS)
//        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    val publicKey = jwkProvider.get("6f8856ed-9189-488f-9011-0ff4b6c08edc").publicKey
    val keySpecPKCS8 = PKCS8EncodedKeySpec(Base64. decode(privKey))
    val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpecPKCS8)

    val token = JWT.create()
        .withClaim("uid", "1234")
        .sign(Algorithm.RSA256(publicKey as RSAPublicKey, privateKey as RSAPrivateKey))

    println(token)



//    val issuer = environment.config.property("jwt.issuer").getString()
//    val audience = environment.config.property("jwt.audience").getString()
//    val myRealm = environment.config.property("jwt.realm").getString()
//    val expireMinute = environment.config.property("jwt.expireMinute").getString().toIntOrNull() ?: 1;
//    val jwkProvider = JwkProviderBuilder(issuer)
//        .cached(10, 24, TimeUnit.HOURS)
//        .rateLimited(10, 1, TimeUnit.MINUTES)
//        .build()


//    val token = genJwtRsa(jwtSecret, "1234").also(::println)
//
//    val chk = chkJwtRsa(jwtSecret, token)!!
//    chk.claims["uid"]!!.asString().also(::println)
//
//    val err = chkJwtRsa("0123456789abcdeX", token)
//    if(err==null) println("Invalid token for Invalid secret")
}

fun genJwtRsa(jwtSecret: String, uid: String): String {
    return JWT.create().withClaim("uid", uid).sign(Algorithm.HMAC256(jwtSecret))
}

fun chkJwtRsa(jwtSecret: String, token: String) = runCatching {
    JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token)
}.getOrElse { null }

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm


fun main() {
    val jwtSecret = "0123456789abcdef"
    val token = genJwt(jwtSecret, "1234").also(::println)

    val chk = chkJwt(jwtSecret, token)!!
    chk.claims["uid"]!!.asString().also(::println)

    val err = chkJwt("0123456789abcdeX", token)
    if(err==null) println("Invalid token for Invalid secret")
}

fun genJwt(jwtSecret: String, uid: String): String {
    return JWT.create().withClaim("uid", uid).sign(Algorithm.HMAC256(jwtSecret))
}

fun chkJwt(jwtSecret: String, token: String)= runCatching {
    JWT.require(Algorithm.HMAC256(jwtSecret)).build().verify(token)
}.getOrElse { null }

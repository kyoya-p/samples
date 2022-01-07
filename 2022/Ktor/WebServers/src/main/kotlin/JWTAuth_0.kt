import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.auth.jwt.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

//    implementation("io.ktor:ktor-server-core:1.6.7") // Serverを作成する場合
//    implementation("io.ktor:ktor-server-netty:1.6.7") // Nettyを使用する場合
//    implementation("io.ktor:ktor-auth:1.6.7") // Basic認証,Digest認証等
//    implementation("io.ktor:ktor-auth-jwt:1.6.7") // JWT認証



// TODO!!
@Suppress("JSON_FORMAT_REDUNDANT")
fun main() {
    embeddedServer(Netty, port = 8080) {
        val jwtIssuer = environment.config.property("jwt.domain").getString()
        val jwtAudience = environment.config.property("jwt.audience").getString()
        val jwtRealm = environment.config.property("jwt.realm").getString()

        val algorithm = Algorithm.HMAC256("secret")
        fun makeJwtVerifier(issuer: String, audience: String) = JWT
            .require(algorithm)
            .withAudience(audience)
            .withIssuer(issuer)
            .build()

        install(Authentication) {
            jwt {
                realm = jwtRealm
                verifier(makeJwtVerifier(jwtIssuer, jwtAudience))
                validate { credential ->
                    if (credential.payload.audience.contains(jwtAudience)) JWTPrincipal(credential.payload) else null
                }
            }
        }

        routing {
            authenticate("auth-digest") {
                get("/") {
                    println("get/")
                    val h = Json { prettyPrint = true }.encodeToString(call.request.headers.toMap())
                    call.respondText("Hello Get Request! \n$h", ContentType.Text.Plain)
                }
            }
        }
    }.start(wait = true)
}
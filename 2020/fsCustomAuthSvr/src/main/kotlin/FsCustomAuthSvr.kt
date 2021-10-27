/*
 Provider of custom token for device authorization
 This module should be run in backend.

 Refer:
 - https://firebase.google.com/docs/admin/setup?hl=ja

    request: GET /customToken?id={deviceId}&pw={devicePassword}

    device/{deviceId}.password == devicePassword ならばカスタム認証pass
    customTokenは、{id, clusterId}

 */

import com.google.cloud.firestore.FirestoreOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import gifts.RspRequest
import gifts.proxy
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

//val db = FirestoreOptions.getDefaultInstance().service!!

@KtorExperimentalLocationsAPI
@Location("/customToken")
data class Credential(val id: String, val pw: String)

@KtorExperimentalLocationsAPI
fun main(args: Array<String>) {
    val port = if (args.size != 1) 8080 else args[0].toInt()

    println("Start Custom token generator Service. Listen port:${port}.")
    // Firestore access with Service Account
    FirebaseApp.initializeApp() // set GOOGLE_APPLICATION_CREDENTIALS=path/to/credential.json

    embeddedServer(Netty, port) {
        install(Locations)
        install(CORS) {
            header("CrossDomain")
            header("X-CSRF-Token")
        }
        routing {

            get<Credential> { credential ->
                println("Requested with credential: $credential")
                val res = createCustomToken(credential)
                println("Response token: $res")
                call.respondText(res, ContentType.Text.Plain)
            }

            /* Just Trial
            get<RspRequest> { rspReq ->
                proxy(rspReq) { it ->
                    (it["headers"] as Map<*, *>).forEach { (k, v) ->
                        k as String
                        v as String
                        println("$k:$v")
                        when (k) {
                            "Content-Length", "Content-Type" -> Unit
                            else -> call.response.header(k, v)
                        }
                    }
                    call.respond(status = HttpStatusCode(400, ""), message = it["body"] as String)
                }
            }
            post<RspRequest> { proxy(it) { call.respondText("aaa") } }
            */
        }
    }.start(wait = true)
}

@KtorExperimentalLocationsAPI
fun createCustomToken(credential: Credential): String {
    val db = FirestoreOptions.getDefaultInstance().service!!

    // Check parameters by Firestore document
    val dev = db.collection("device").document(credential.id).get().get()?.data
    val devPw = dev?.get("password") as String?
    if (devPw == null || devPw != credential.pw) return ""

    val devClusterId = dev?.get("cluster") as String? ?: return ""

    // Make Custom Token with custom Claims
    val additionalClaims = mapOf(
            "id" to credential.id,
            "cluster" to devClusterId,
    )

    val mAuth = FirebaseAuth.getInstance()
        val serviceUserId = "firebase-adminsdk-rc191@road-to-iot.iam.gserviceaccount.com"
    val customJwtToken = mAuth.createCustomToken(serviceUserId, additionalClaims)
    //customJwtToken.split(".").take(2).map { println(String(Base64.getDecoder().decode(it))) }

    return customJwtToken
}


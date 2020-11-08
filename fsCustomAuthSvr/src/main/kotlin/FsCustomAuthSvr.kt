/*
 Provider of custom token for devide authorization
 This module should be run in backend.

 Refer:
 - https://firebase.google.com/docs/admin/setup?hl=ja

    request: GET /customToken?id={deviceId}&pw={devicePassword}

    device/{deviceId}.password == devicePassword ならばカスタム認証pass
    customTokenは、{id, clusterId}

 */

//package fsCustomAuthSvr

import com.google.cloud.firestore.FirestoreOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import gifts.RspRequest
import gifts.proxy
import java.util.*


import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.apache.http.HttpStatus

val db = FirestoreOptions.getDefaultInstance().getService()!!

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
        routing {
            get<Credential> { credential ->
                val res = createCustomToken(credential)
                println("Res=${res}")
                call.respondText(res, ContentType.Text.Plain)
            }

            get<RspRequest> {
                proxy(it) {
                    (it["headers"] as Map<String, String>).forEach { (k, v) ->
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
        }
    }.start(wait = true)
}


//@Serializable
//data class CustomClaimes(val id:String, val clusterId:String,)

@KtorExperimentalLocationsAPI
fun createCustomToken(credential: Credential): String {
    println("Requested with credential: $credential")

    // Check parameters by Firestore document
    val devPasswd = db.collection("device").document(credential.id).get().get()?.data?.get("password") as String?
    if (devPasswd == null || devPasswd != credential.pw) return ""

    // Get clusterId of this device
    val clusterId = db.collection("group").whereEqualTo("member.${credential.id}", true).limit(1).get().get()?.documents?.get(0)?.id
    if (clusterId == null) return ""

    // Make Custom Token with custom Claims
    val additionalClaims = mapOf(
            "id" to credential.id,
            "clusterId" to clusterId,
    )

    val mAuth = FirebaseAuth.getInstance()
    val serviceUserId = "firebase-adminsdk-rc191@road-to-iot.iam.gserviceaccount.com"
    val customJwtToken = mAuth.createCustomToken(serviceUserId, additionalClaims)
    customJwtToken.split(".").take(2).map { println(String(Base64.getDecoder().decode(it))) }

    return customJwtToken
}


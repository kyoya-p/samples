package fsCustomAuthSvr

import AesEncriptor.decript
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import java.io.ByteArrayInputStream
import java.util.*


/*
 カスタム認証ロジック(GDVMではデバイス認証のために使用)
 通常バックエンドで実行する
 https://firebase.google.com/docs/admin/setup?hl=ja
 */

// Firebase Admin SDK の初期化

fun main() {
    val serviceUserId = "firebase-adminsdk-rc191@road-to-iot.iam.gserviceaccount.com" // from SecretKey.json

    /*val options = FirebaseOptions.Builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .setServiceAccountId(serviceUserId)
            .build()
     */

    val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(ByteArrayInputStream(myFirebaseAppKey.decript("Soft2ceram").toByteArray())))
            .build()

    FirebaseApp.initializeApp(options)

    println("Start Coustome Authorizer Service.")

    // Custom Token作成
    val additionalClaims: MutableMap<String, Any> = HashMap()
    additionalClaims["agentId"] = "aaa"
    additionalClaims["clusterId"] = "G1"

    val mAuth = FirebaseAuth.getInstance()
    val customJwtToken = mAuth.createCustomToken(serviceUserId, additionalClaims)
    customJwtToken.split(".").take(2).map { println(String(Base64.getDecoder().decode(it))) }

    //TODO
}


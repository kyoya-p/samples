plugins {
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}


dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    implementation("io.ktor:ktor-server-core:1.6.7") // Serverを作成する場合
    implementation("io.ktor:ktor-server-netty:1.6.7") // Nettyを使用する場合
    implementation("io.ktor:ktor-network-tls-certificates:1.6.7") // TLSサポート
    implementation("io.ktor:ktor-auth:1.6.7") // Basic認証,Digest認証等
    implementation("io.ktor:ktor-auth-jwt:1.6.7") // JWT認証

    implementation("ch.qos.logback:logback-classic:1.2.10") // ログを取得する場合
    //implementation("org.apache.logging.log4j:log4j:2.17.1")  //脆弱性チェック
}

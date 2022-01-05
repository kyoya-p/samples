plugins {
//    application
    kotlin("jvm") version "1.6.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-core:1.6.7") // Serverを作成する場合
    implementation("io.ktor:ktor-server-netty:1.6.7") // Nettyを使用する場合
    implementation("io.ktor:ktor-auth:1.6.7")

    implementation("ch.qos.logback:logback-classic:1.2.10") // ログを取得する場合

}

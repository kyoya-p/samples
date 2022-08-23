plugins {
    kotlin("jvm") version "1.7.10" // https://kotlinlang.org/docs/releases.html
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-cio:2.0.3") // CIO-Clientの場合
    implementation("io.ktor:ktor-client-auth:2.0.3")

    //implementation("ch.qos.logback:logback-classic:1.2.9") //ログを取る場合
}
plugins {
    kotlin("jvm") version "1.6.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-client-cio:1.6.7") // CIO-Clientの場合
    implementation("io.ktor:ktor-client-auth-jvm:1.6.7")

    //implementation("ch.qos.logback:logback-classic:1.2.9") //ログを取る場合
}
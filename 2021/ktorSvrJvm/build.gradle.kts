plugins {
    application
    id("org.jetbrains.kotlin.jvm") version "1.5.31" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
}

group = "org.example"
version = "1.0-SNAPSHOT"

val ktor_version = "1.6.4" // https://ktor.io/

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.31")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-websockets:$ktor_version")
    implementation("io.ktor:ktor-network-tls-certificates:$ktor_version")
}


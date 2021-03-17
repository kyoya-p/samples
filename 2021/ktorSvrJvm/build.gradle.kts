plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.31"
}

group = "org.example"
version = "1.0-SNAPSHOT"

val ktor_version = "1.5.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-websockets:$ktor_version")

    implementation("io.ktor:ktor-network-tls-certificates:$ktor_version")
}

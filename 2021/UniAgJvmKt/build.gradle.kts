plugins {
    application
    kotlin("jvm") version "1.4.31"
    kotlin("plugin.serialization") version "1.4.31"
    //id("com.github.johnrengelman.shadow") version "6.1.0"
}

val serializationVersion = "1.1.0" // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization
val ktorVersion = "1.5.2" // https://mvnrepository.com/artifact/io.ktor

group = "gdvmAgentService"
version = "1.0-SNAPSHOT"
application.mainClass.set("io.ktor.server.netty.EngineMain")

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    //implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
    implementation("io.ktor:ktor-server:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-websockets:$ktorVersion")
    //implementation("io.ktor:ktor-locations:$ktorVersion")
    implementation("io.ktor:ktor-network-tls:$ktorVersion")
    implementation("io.ktor:ktor-network-tls-certificates:$ktorVersion")  // https://ktor.io/docs/self-signed-certificate.html
}






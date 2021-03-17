plugins {
    application
    id("org.jetbrains.kotlin.jvm") version "1.4.31" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
    id("com.github.johnrengelman.shadow") version "6.1.0" // https://imperceptiblethoughts.com/shadow
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


application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

tasks.withType<Jar> {
    manifest {
        attributes(mapOf("Main-Class" to application.mainClass))
    }
    copy{from}
}

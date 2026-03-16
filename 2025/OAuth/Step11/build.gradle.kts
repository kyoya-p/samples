plugins {
    kotlin("jvm") version "2.1.0"
//    kotlin("plugin.serialization") version "2.1.0"
    application
//    id("com.github.johnrengelman.shadow") version "8.1.1" // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
}

//group = "org.example"
//version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
//    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
//    google()
}

application { mainClass = "MainKt" }

val ktor_version = "3.0.3"

dependencies {
//    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
//    implementation("io.ktor:ktor-client-auth:$ktor_version")
//    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
//    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-cio:$ktor_version")
//    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
}

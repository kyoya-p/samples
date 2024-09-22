plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization
    application
}

application.mainClass.set("MainKt")
repositories { mavenCentral() }
dependencies {
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("com.squareup.okio:okio:3.9.1")
}

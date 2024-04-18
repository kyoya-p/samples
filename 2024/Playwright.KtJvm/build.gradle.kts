plugins {
    kotlin("jvm") version "1.9.23"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1" // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
}
repositories { mavenCentral() }
dependencies {
    implementation("com.microsoft.playwright:playwright:1.43.0")  // https://mvnrepository.com/artifact/com.microsoft.playwright/playwright
    implementation("io.ktor:ktor-server-cio:2.3.10") // https://mvnrepository.com/artifact/io.ktor/ktor-server-cio
}

application {
    mainClass.set("MainKt")
}


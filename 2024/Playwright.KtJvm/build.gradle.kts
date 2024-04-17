plugins {
    kotlin("jvm") version "1.9.23"
//    kotlin("multiplatform") version "1.9.23"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1" // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow
}
repositories { mavenCentral() }
dependencies {
    implementation("com.microsoft.playwright:playwright:1.43.0")  // https://mvnrepository.com/artifact/com.microsoft.playwright/playwright
//            testImplementation(kotlin("test"))
}

application {
    mainClass.set("MainKt")
}


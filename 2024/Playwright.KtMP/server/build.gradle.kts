plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
    kotlin("plugin.serialization") version "1.9.23"
}

group = "jp.wjg.shokkaa"
version = "1.0.0"
application {
    mainClass.set("MainKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["development"] ?: "false"}")
}

dependencies {
//    implementation(projects.shared)
//    implementation(projects.composeApp)
//    implementation(libs.logback)
//    implementation(libs.ktor.server.cio)
//    implementation(libs.ktor.server.netty)

    implementation("com.microsoft.playwright:playwright:1.43.0")  // https://mvnrepository.com/artifact/com.microsoft.playwright/playwright
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-json

    val ktor_version="2.3.10"
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-server-cio:$ktor_version") // https://mvnrepository.com/artifact/io.ktor/ktor-server-cio
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
}
plugins {
    kotlin("jvm") version "2.4.0"
    kotlin("plugin.serialization") version "2.4.0"
    application
}

group = "ai.koog.samples"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // JetBrains Koog core stable 1.0.0 and modern executors (using OkHttp instead of Apache5 for proxy support)
    implementation("ai.koog:koog-agents-jvm:1.0.0") {
        exclude(group = "io.ktor", module = "ktor-client-apache5")
    }
    implementation("io.ktor:ktor-client-okhttp:3.3.3")
    implementation("ai.koog:prompt-executor-google-client:1.0.0-beta")
    implementation("ai.koog:prompt-executor-llms-all:1.0.0-beta")
    
    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    
    // Playwright for Java/Kotlin
    implementation("com.microsoft.playwright:playwright:1.61.0")
    
    // Kotlin IO and Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
}

application {
    mainClass.set("ai.koog.samples.ComputerUseDemoKt")
    applicationDefaultJvmArgs = listOf(
        "-Dhttp.proxyHost=192.168.81.174",
        "-Dhttp.proxyPort=3080",
        "-Dhttps.proxyHost=192.168.81.174",
        "-Dhttps.proxyPort=3080",
        "-Djavax.net.ssl.trustStore=C:\\Users\\sharp\\dev\\.gemini\\skills\\sclan\\assets\\RootCA-proxy-nara.jks",
        "-Djavax.net.ssl.trustStorePassword=changeit"
    )
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn")
    }
}

tasks.named<JavaExec>("run") {
    jvmArgs = listOf(
        "-Dhttp.proxyHost=192.168.81.174",
        "-Dhttp.proxyPort=3080",
        "-Dhttps.proxyHost=192.168.81.174",
        "-Dhttps.proxyPort=3080",
        "-Djavax.net.ssl.trustStore=C:\\Users\\sharp\\dev\\.gemini\\skills\\sclan\\assets\\RootCA-proxy-nara.jks",
        "-Djavax.net.ssl.trustStorePassword=changeit"
    )
}

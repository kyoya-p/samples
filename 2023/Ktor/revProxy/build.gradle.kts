val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.8.22"
    id("io.ktor.plugin") version "2.3.1"
}

group = "samples"
version = "0.0.1"
application {
    mainClass.set("RproxyKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    mavenCentral()
}

dependencies {
    val ktor_version = "2.3.1"
    val kotlin_version = "1.8.22"
    val logback_version = "1.2.11"

    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
//    implementation("io.ktor:ktor-http:$ktor_version")
    implementation("ch.qos.logback:logback-classic:$logback_version")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
}
plugins {
    kotlin("jvm") version "1.8.0"
    id("io.ktor.plugin") version "2.3.0"
}

repositories {
    mavenCentral()
}

dependencies {
    val kotlin_version = "1.8.0"

    val ktor_version = "2.3.0"
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")

    implementation("io.ktor:ktor-serialization-kotlinx-xml:$ktor_version")


    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlin_version")
    testImplementation("ch.qos.logback:logback-classic:1.4.7")
}

group = "org.example"
version = "1.0-SNAPSHOT"


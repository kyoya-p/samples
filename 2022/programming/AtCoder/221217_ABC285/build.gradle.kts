import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.10" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
    kotlin("plugin.serialization") version "1.8.10" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

//    val ktor_version="2.2.3" // https://mvnrepository.com/artifact/io.ktor/ktor-server-core
//    testImplementation("io.ktor:ktor-client-core:$ktor_version")
//    testImplementation("io.ktor:ktor-client-cio:$ktor_version")
//    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
//    testImplementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
//    testImplementation("io.ktor:ktor-serialization-kotlinx-xml:$ktor_version")
    testImplementation("org.slf4j:slf4j-simple:2.0.6") // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple

//    testImplementation("io.github.pdvrieze.xmlutil:core:0.84.3") // https://mvnrepository.com/artifact/io.github.pdvrieze.xmlutil/core
//    testImplementation("io.github.pdvrieze.xmlutil:serialization:0.84.3")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.languageVersion = "1.3"
}

application {
    mainClass.set("MainKt")
}
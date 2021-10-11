plugins {
    kotlin("jvm") version "1.5.31"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.5.31"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib","1.5.31"))
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.5.31")

    implementation("io.ktor:ktor-client-core:1.6.4")  // https://mvnrepository.com/artifact/io.ktor/ktor-client-core-jvm
    implementation("io.ktor:ktor-client-cio:1.6.4") // https://mvnrepository.com/artifact/io.ktor/ktor-client-cio-jvm
    implementation("io.ktor:ktor-client-serialization:1.6.4") // https://mvnrepository.com/artifact/io.ktor/ktor-client-serialization
    implementation("io.ktor:ktor-client-json:1.6.4") // https://mvnrepository.com/artifact/io.ktor/ktor-client-json
}
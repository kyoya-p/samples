plugins {
    kotlin("jvm") version "1.7.21"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    val ktor_version = "2.3.0"
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-client-apache:$ktor_version")
    implementation("io.ktor:ktor-client-json:$ktor_version")
    implementation("io.ktor:ktor-client-serialization:$ktor_version")
}

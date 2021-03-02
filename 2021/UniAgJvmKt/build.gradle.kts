plugins {
    kotlin("jvm") version "1.4.31"
    //kotlin("serialization")
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("io.ktor:ktor-server-core:1.5.2")
    implementation("io.ktor:ktor-server:1.5.2")
    implementation("io.ktor:ktor-server-cio:1.5.2")
}

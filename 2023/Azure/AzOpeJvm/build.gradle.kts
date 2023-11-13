plugins {
    kotlin("jvm") version "1.9.10"
    kotlin("plugin.serialization") version "1.9.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("com.azure:azure-identity:1.11.0")
    implementation("com.azure.resourcemanager:azure-resourcemanager:2.31.0")
}

kotlin {
    jvmToolchain(8)
}

application {
    mainClass.set("MainKt")
}

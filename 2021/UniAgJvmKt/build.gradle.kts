plugins {
    kotlin("jvm") version "1.4.31"
    kotlin("plugin.serialization") version "1.4.31"
    application
}

val serializationVersion = "1.1.0" // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization
val ktorVersion = "1.5.2" // https://mvnrepository.com/artifact/io.ktor

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    //implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
    implementation("io.ktor:ktor-server:$ktorVersion")
    implementation("io.ktor:ktor-server-cio:$ktorVersion")
    implementation("io.ktor:ktor-locations:$ktorVersion")
}

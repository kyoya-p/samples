plugins {
    kotlin("jvm") version "2.2.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("ai.koog:koog-agents:0.3.0") // https://mvnrepository.com/artifact/ai.koog/koog-agents
    implementation("org.jetbrains.kotlinx:kotlinx-io:0.1.16") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-io/

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
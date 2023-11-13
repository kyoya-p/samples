plugins {
    kotlin("jvm") version "1.9.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1" // https://plugins.gradle.org/plugin/com.github.johnrengelman.shadow

}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.mongodb:mongo-java-driver:3.12.14") // https://mvnrepository.com/artifact/org.mongodb/mongo-java-driver
    testImplementation(kotlin("test"))
}

tasks.test { useJUnitPlatform() }
kotlin { jvmToolchain(8) }

application {
    mainClass.set("MainKt")
}
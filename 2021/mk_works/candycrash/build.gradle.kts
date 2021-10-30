plugins {
    application
    java
    id("org.jetbrains.kotlin.jvm") version "1.5.31" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.jvm
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("junit:junit:4.13.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
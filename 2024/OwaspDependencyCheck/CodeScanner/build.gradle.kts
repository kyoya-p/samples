plugins {
    kotlin("jvm") version "1.9.21"
    application
    id("org.owasp.dependencycheck") version "11.1.0"
}

group = "com.security"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.json:json:20231013")
    implementation("org.jsoup:jsoup:1.17.1")
    implementation("org.owasp:dependency-check-core:8.4.2")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

application {
    mainClass.set("MainKt")
}

dependencyCheck {
    format = "json"
}
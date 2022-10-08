import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("org.testng:testng:7.1.0")
}

tasks.test { useJUnitPlatform() }

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.0"
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("javax.xml.stream:stax-api:1.0-2")  // https://mvnrepository.com/artifact/javax.xml.stream/stax-api
    implementation("com.fasterxml.woodstox:woodstox-core:6.5.1") // https://mvnrepository.com/artifact/com.fasterxml.woodstox/woodstox-core

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(11)
}

application {
    mainClass.set("MainKt")
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
//    languageVersion = "1.9"
}
plugins {
    //kotlin("jvm") version "1.5.10"
    kotlin("jvm") version "1.6.21" // https://kotlinlang.org/docs/releases.html
    idea
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.6.20")
    implementation("org.jetbrains.kotlin:kotlin-reflect:1.6.20")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0-native-mt")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.3.2") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime

    // JNA
    implementation("net.java.dev.jna:jna:5.10.0")
    implementation("net.java.dev.jna:jna-platform:5.10.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks.test {
    useJUnitPlatform()
}



val compileTestKotlin: org.jetbrains.kotlin.gradle.tasks.KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    languageVersion = "1.7"
    freeCompilerArgs = listOf("-Xcontext-receivers")
}


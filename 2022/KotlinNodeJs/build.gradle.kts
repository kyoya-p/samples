plugins {
    kotlin("js") version "1.6.21"
}

group = "me.kyoya"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    //implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.6.2")
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test-js:1.6.2")
}

kotlin {
    js(LEGACY) {
        binaries.executable()
        nodejs {

        }
    }
}
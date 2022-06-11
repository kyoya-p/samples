plugins {
    kotlin("js") version "1.6.21"
    //id("kotlin2js") version "1.7.0"
}

group = "me.kyoya"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.6.2")
    testImplementation(kotlin("test"))
    implementation(kotlin("stdlib-js"))
}

kotlin {
    js(LEGACY) {
        binaries.executable()
        nodejs {

        }
    }
}
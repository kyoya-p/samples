plugins {
    kotlin("multiplatform") version "2.0.0"

    kotlin("plugin.serialization") version "2.0.0"
}

repositories {
//    mavenLocal()
    google()
    mavenCentral()
}

kotlin {
    js {
        browser {}
        binaries.executable()
    }
    sourceSets {
        jsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.8.1") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
            implementation("dev.gitlive:firebase-common:1.13.0")
            implementation("dev.gitlive:firebase-auth:1.13.0")
            implementation("dev.gitlive:firebase-firestore:1.13.0")
        }
    }
}

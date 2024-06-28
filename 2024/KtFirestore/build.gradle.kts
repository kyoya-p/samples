plugins {
    kotlin("multiplatform") version "2.0.0"
//    kotlin("plugin.serialization") version "2.0.0"
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

repositories {
//    google()
    mavenCentral()
}

kotlin {
    js {
        browser {}
        binaries.executable()
    }
    sourceSets {
        jsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
            implementation("dev.gitlive:firebase-firestore:1.13.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-firestore
            implementation("org.jetbrains.kotlinx:kotlinx-html:0.11.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-html
        }
    }
}

//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
}

repositories {
    google()
    mavenCentral()
}

//tasks.withType<KotlinCompile> {
//    kotlinOptions.freeCompilerArgs = listOf("-Xcontext-receivers")
//}

kotlin {
    js {
        browser {}
        binaries.executable()
    }
    sourceSets {
        jsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
            implementation("dev.gitlive:firebase-firestore:1.13.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-firestore
            implementation("dev.gitlive:firebase-auth:1.13.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-firestore
            implementation("org.jetbrains.kotlinx:kotlinx-html:0.11.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-html
            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
            implementation(project(":shared"))
        }
    }
}


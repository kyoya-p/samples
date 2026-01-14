@file:OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
            implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
            implementation("dev.gitlive:firebase-firestore:2.1.0")
        }
        
        commonTest.dependencies {
            implementation(kotlin("test"))
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
        
        jvmMain.dependencies {
            implementation("com.google.firebase:firebase-admin:9.4.2")
        }
        
        // Amper directory mapping
        commonMain {
            kotlin.srcDir("src")
        }
        jvmMain {
            kotlin.srcDir("src@jvm")
        }
        commonTest {
            kotlin.srcDir("test")
        }
        val jvmTest by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

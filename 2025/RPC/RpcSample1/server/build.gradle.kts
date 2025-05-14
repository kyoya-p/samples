@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl


plugins {
    kotlin("multiplatform") version "2.1.20"
    id("org.jetbrains.kotlinx.rpc.plugin") version "0.6.2"  // https://plugins.gradle.org/plugin/org.jetbrains.kotlinx.rpc.plugin
    kotlin("plugin.serialization") version "2.1.20"
}

repositories {
    mavenCentral()
}

kotlin {
//    jvm()
    js {
        nodejs()
        binaries.executable()
    }
//    wasmJs {
//        nodejs()
//    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":service"))
            implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-server:0.6.2")
//            implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-client:0.6.2")
        }

//        jvmMain.dependencies {
//            implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-server-jvm:0.6.2")
//        }

        jsMain.dependencies {
        }
    }
}

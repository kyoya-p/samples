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
    jvm()
//    mingwX64()
    linuxX64()
    js{browser()}
    wasmJs{browser()}
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-server:0.6.2")
            implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-ktor-server:0.6.2")
            implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-serialization-json:0.6.2")
        }
    }
}

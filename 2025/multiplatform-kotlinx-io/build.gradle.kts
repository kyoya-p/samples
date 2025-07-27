import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.2.0" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.multiplatform
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    js { binaries.executable(); nodejs() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { binaries.executable(); nodejs() }
    mingwX64 { binaries.executable() }
    linuxX64 { binaries.executable() }

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
        }
    }
}

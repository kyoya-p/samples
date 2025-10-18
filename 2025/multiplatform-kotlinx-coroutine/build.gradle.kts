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
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2") // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
        }
    }
}

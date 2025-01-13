import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

kotlin {
    jvm()
    js { binaries.executable(); nodejs() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { binaries.executable(); nodejs() }
    mingwX64 { binaries.executable() }
    linuxX64 { binaries.executable() }

    sourceSets {
        val kotlin_coroutine = "1.10.1"  // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-core
        val okio_version="3.10.2"         // https://mvnrepository.com/artifact/com.squareup.okio/okio
        val commonMain by getting
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlin_coroutine")
            implementation("com.squareup.okio:okio:$okio_version")
        }
        val jsMain by getting
        jsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$kotlin_coroutine")
            implementation("com.squareup.okio:okio-js:$okio_version")
            implementation("com.squareup.okio:okio-nodefilesystem:$okio_version")
        }
        val wasmJsMain by getting
        wasmJsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-wasm-js:$kotlin_coroutine")
            implementation("com.squareup.okio:okio-wasm-js:$okio_version")
            implementation("com.squareup.okio:okio-fakefilesystem-wasm-js:$okio_version")
        }

        val mingwX64Main by getting
        mingwX64Main.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-mingwx64:$kotlin_coroutine")
            implementation("com.squareup.okio:okio-mingwx64:$okio_version")
        }
        val linuxX64Main by getting
        linuxX64Main.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-linuxx64:$kotlin_coroutine")
            implementation("com.squareup.okio:okio-linuxx64:$okio_version")
        }
    }
}

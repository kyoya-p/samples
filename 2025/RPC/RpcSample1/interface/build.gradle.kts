import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl


plugins {
    kotlin("multiplatform") version "2.1.20"
    kotlin("plugin.serialization") version "2.1.20" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvm()
    js { binaries.executable(); nodejs() }
    mingwX64 { binaries.executable() }
    linuxX64 { binaries.executable() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { binaries.executable(); nodejs() }

    sourceSets {
        val ktor_version = "3.1.3"         // https://mvnrepository.com/artifact/io.ktor/ktor-client-core
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-server:0.6.2")
            implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-serialization-json:0.6.2")

            // Transport implementation for Ktor
//            implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-ktor-client:0.6.2")
            implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-ktor-server:0.6.2")

            // Ktor API
//            implementation("io.ktor:ktor-client-cio-jvm:$ktor_version")
            implementation("io.ktor:ktor-server-netty-jvm:$ktor_version")
        }
        jsMain.dependencies {
        }
        mingwX64Main.dependencies {
        }
        linuxX64Main.dependencies {
        }
        wasmJsMain.dependencies {
            implementation("io.ktor:ktor-client-core:$ktor_version-wasm2")
        }
    }
}

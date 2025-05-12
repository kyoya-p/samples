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
            implementation("io.ktor:ktor-client-cio:$ktor_version")
            implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
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

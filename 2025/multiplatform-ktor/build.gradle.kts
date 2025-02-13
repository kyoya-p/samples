import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl


plugins {
    kotlin("multiplatform") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization
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
    js { nodejs(); binaries.executable() }
    mingwX64 { binaries.executable() }
    linuxX64 { binaries.executable() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { binaries.executable(); nodejs() }

    sourceSets {
        val ktor_version = "3.1.0"         // https://mvnrepository.com/artifact/io.ktor/ktor-client-core
        val commonMain by getting
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-cio:$ktor_version")
            implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
        }
        val jsMain by getting
        jsMain.dependencies {
            implementation("io.ktor:ktor-client-js:$ktor_version")
        }
        val mingwX64Main by getting
        mingwX64Main.dependencies {
        }
        val linuxX64Main by getting
        linuxX64Main.dependencies {
            implementation("io.ktor:ktor-client-cio-linuxx64:$ktor_version")
        }
        val wasmJsMain by getting
        wasmJsMain.dependencies {
            implementation("io.ktor:ktor-client-core:$ktor_version-wasm2")
        }
    }
}

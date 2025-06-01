import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.2.0-RC"
    kotlin("plugin.serialization") version "2.2.0-RC" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization
    id("io.kotest.multiplatform") version "5.9.1" // https://plugins.gradle.org/plugin/io.kotest.multiplatform
//    id("io.kotest.multiplatform") version "6.0.0.M3"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

val ktor_version = "3.1.0"  // https://mvnrepository.com/artifact/io.ktor/ktor-client-core
val kotest_version = "5.9.1"  // https://plugins.gradle.org/plugin/io.kotest.multiplatform
//val kotest_version = "6.0.0.M4"  // https://plugins.gradle.org/plugin/io.kotest.multiplatform

kotlin {
    jvm()
    js { binaries.executable();nodejs() }
    mingwX64 { binaries.executable() }
    linuxX64 { binaries.executable() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { binaries.executable(); nodejs() }

    sourceSets {
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-cio:$ktor_version")
            implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
            implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")

            implementation("io.ktor:ktor-server-cio:${ktor_version}")
            implementation("io.ktor:ktor-server-content-negotiation:${ktor_version}")
        }
        wasmJsMain.dependencies {
            implementation("io.ktor:ktor-client-core:$ktor_version-wasm2")
        }

        commonTest.dependencies {
            implementation("io.kotest:kotest-framework-engine:$kotest_version") //https://mvnrepository.com/artifact/io.kotest/kotest-framework-engine
            implementation("io.ktor:ktor-server-cio:${ktor_version}")
            implementation("io.ktor:ktor-server-content-negotiation:${ktor_version}")
        }
        jvmTest.dependencies {
            implementation("io.kotest:kotest-runner-junit5:$kotest_version") // https://mvnrepository.com/artifact/io.kotest/kotest-runner-junit5
        }


        jvm {
            testRuns.named("test") { executionTask.configure { useJUnitPlatform() } }
        }
    }
}

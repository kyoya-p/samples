import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalMainFunctionArgumentsDsl

plugins {
    kotlin("multiplatform") version "2.0.20"
    id("io.kotest.multiplatform") version "5.9.1"
}
repositories { mavenCentral() }
kotlin {
    js {
        nodejs {
            @OptIn(ExperimentalMainFunctionArgumentsDsl::class) passProcessArgvToMainFunction()
//            this.runTask {
//                this.nodeJs.versions.dukat.version = "0.0.28"
//            }
        }
        binaries.executable()
//        @OptIn(ExperimentalKotlinGradlePluginApi::class)
//        compilerOptions {
//            target.set("es2015")
//        }
    }
//    @OptIn(ExperimentalWasmDsl::class)
//    wasmJs{
//        nodejs()
//        binaries.executable()
//    }
    sourceSets {
        jsMain.dependencies {
//            implementation("io.ktor:ktor-client-core:2.3.12")
//            implementation("io.ktor:ktor-client-js:2.3.12")
//            implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
//            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
//            implementation("com.squareup.okio:okio:3.9.1")
            implementation(npm("@google/generative-ai", "0.19.0"))
        }
        commonTest.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("io.kotest:kotest-framework-engine:5.9.1")
            implementation("io.kotest:kotest-assertions-core:5.9.1")
            implementation("io.kotest:kotest-property:5.9.1")
        }
    }
}
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalMainFunctionArgumentsDsl

plugins { kotlin("multiplatform") version "2.0.20" }
repositories { mavenCentral() }
kotlin {
    js {
        nodejs {
            @OptIn(ExperimentalMainFunctionArgumentsDsl::class)
            passProcessArgvToMainFunction()
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
//        wasmJsMain.dependencies {
//            implementation("io.ktor:ktor-client-js:2.3.12")
//            implementation("com.squareup.okio:okio:3.9.1")
//            implementation(npm("@google/generative-ai", "0.19.0"))
//        }
    }
}
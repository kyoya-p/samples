import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalMainFunctionArgumentsDsl

plugins {
    kotlin("multiplatform") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.0" // https://plugins.gradle.org/plugin/org.jetbrains.kotlin.plugin.serialization
}

repositories {
    mavenCentral()
}

kotlin {
    js {
        nodejs {
            @OptIn(ExperimentalMainFunctionArgumentsDsl::class)
            passProcessArgvToMainFunction()
//            this.runTask {
                // バージョンを指定してあげる
//                this.nodeJs.versions.dukat.version = "0.0.28"
//            }
        }
        binaries.executable()
//        @OptIn(ExperimentalKotlinGradlePluginApi::class)
//        compilerOptions {
//            target.set("es2015")
//        }
    }
    sourceSets {
        jsMain.dependencies {
            implementation("io.ktor:ktor-client-core:2.3.12")
            implementation("io.ktor:ktor-client-js:2.3.12")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
            implementation("com.squareup.okio:okio:3.9.1")
            implementation(npm("@google/generative-ai", "0.19.0"))
        }
    }
}
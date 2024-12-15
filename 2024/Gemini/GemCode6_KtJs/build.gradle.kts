import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalMainFunctionArgumentsDsl

plugins {
    kotlin("multiplatform") version "2.0.20"
    id("io.kotest.multiplatform") version "5.9.1"
}
repositories { mavenCentral() }
kotlin {
    js {
        nodejs { }
        binaries.executable()
    }
    sourceSets {
        commonMain.dependencies {
            implementation("com.squareup.okio:okio:3.9.1")
            implementation("io.ktor:ktor-client-core:2.3.12")
//            implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
//            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
        }

        jsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation(npm("@google/generative-ai", "0.19.0"))
            implementation("com.squareup.okio:okio-nodefilesystem:3.9.1")
            implementation("io.ktor:ktor-client-js:2.3.12")
        }

        commonTest.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
            implementation("io.kotest:kotest-framework-engine:5.9.1")
            implementation("io.kotest:kotest-assertions-core:5.9.1")
            implementation("io.kotest:kotest-property:5.9.1")

            implementation("com.squareup.okio:okio:3.9.1")
            implementation("com.squareup.okio:okio-fakefilesystem:3.9.1")
        }
    }
}
plugins {
    id("org.jetbrains.kotlin.js") version "1.4.30"
    kotlin("plugin.serialization") version "1.4.30"
}

group = "org.example"
version = "1.0-SNAPSHOT"

val serializationVersion = "1.0.0-RC"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("io.ktor:ktor-client-core:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
    implementation("dev.gitlive:firebase-auth:1.2.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-auth
    implementation("dev.gitlive:firebase-firestore:1.2.0") // https://mvnrepository.com/artifact/dev.gitlive/firebase-firestore
}

kotlin {
    js {
        browser {
            webpackTask {
                cssSupport.enabled = true
            }

            runTask {
                cssSupport.enabled = true
            }

            testTask {
                useKarma {
                    useChromeHeadless()
                    webpackConfig.cssSupport.enabled = true
                }
            }
        }
        binaries.executable()
    }
}
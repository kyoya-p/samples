plugins {
    id("org.jetbrains.kotlin.js") version "1.6.10"
    kotlin("plugin.serialization") version "1.6.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven") }
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime-js:1.0-M1-1.4.0-rc")

    implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.2")
}

kotlin {
    js(IR) {
        browser {
            webpackTask {
                cssSupport.enabled = true
                //output.libraryTarget = "commonjs2"
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
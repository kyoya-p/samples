plugins {
    id("org.jetbrains.kotlin.js") version "1.7.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))

    implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.3") //https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-html-js
    implementation("io.ktor:ktor-client-js:1.6.7") // https://mvnrepository.com/artifact/io.ktor/ktor-client-js
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
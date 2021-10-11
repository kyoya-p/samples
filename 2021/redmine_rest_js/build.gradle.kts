plugins {
    id("org.jetbrains.kotlin.js") version "1.6.0-M1"
}

// issue
// https://youtrack.jetbrains.com/issue/KT-48477

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
}

kotlin {
    js {
        nodejs {
        }
        binaries.executable()
        browser()
    }
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
plugins {
    id("org.jetbrains.kotlin.js") version "1.6.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    //implementation("io.ktor:ktor-client-js:1.6.7") // https://mvnrepository.com/artifact/io.ktor/ktor-client-js
    //implementation("org.webjars:cryptojs:3.1.2") // https://mvnrepository.com/artifact/org.webjars/cryptojs
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

tasks["build"].doLast {
    copy {
        from("$buildDir/distributions")
        into("$projectDir/dist")
    }
}

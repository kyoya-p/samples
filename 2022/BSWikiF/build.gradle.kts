plugins {
    id("org.jetbrains.kotlin.js") version "1.6.10"
    id("org.jetbrains.compose") version "1.2.0-alpha01-dev620"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("org.jetbrains.kotlinx:kotlinx-html-js:0.7.3")
    implementation(compose.web.core)
    implementation(compose.runtime)
}

kotlin {
    js(IR) {
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


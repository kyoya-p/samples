plugins {
    kotlin("js") version "1.6.10"
}

group = "me.kyoya"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    //maven("https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven")
}

dependencies {
    //testImplementation(kotlin("test"))
    //implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.2")
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }
}
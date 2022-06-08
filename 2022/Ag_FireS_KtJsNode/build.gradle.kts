plugins {
    kotlin("js") version "1.6.21"
}

group = "me.kyoya"
version = "1.0-SNAPSHOT"

repositories {
    //jcenter()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-nodejs:0.0.7")
}

kotlin {
    js(IR) {
        binaries.executable()
        nodejs {

        }
    }
}
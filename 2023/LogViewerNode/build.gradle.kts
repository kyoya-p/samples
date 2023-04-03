plugins {
    kotlin("js") version "1.8.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-nodejs:0.0.7")
    implementation(npm("@types/node", "18.15.11")) // https://www.npmjs.com/package/@types/node
}

kotlin {
    js {
        binaries.executable()
        nodejs {

        }
    }
}
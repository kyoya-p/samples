plugins {
    kotlin("js") version "1.8.20"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-nodejs:0.0.7")
    implementation("io.ktor:ktor-client-core-js:2.3.0")
    implementation("io.ktor:ktor-client-cio:2.3.0")
    implementation("io.ktor:ktor-client-cio-js:2.3.0")

    testImplementation(kotlin("test"))

}

kotlin {
    js {
        binaries.executable()
        nodejs {
        }
    }
}
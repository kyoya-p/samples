plugins {
    id("org.jetbrains.kotlin.js") version "1.4.31"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    implementation("io.ktor:ktor-server:1.5.2")
}

kotlin {
    js {
        nodejs {
        }
        binaries.executable()
    }
}
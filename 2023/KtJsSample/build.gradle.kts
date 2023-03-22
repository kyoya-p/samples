plugins {
    kotlin("js") version "1.8.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    js {
        binaries.executable()
        nodejs {

        }
    }
}